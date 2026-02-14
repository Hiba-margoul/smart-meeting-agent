import asyncio
import logging
import numpy as np
import json
import os
import sys
import requests
from concurrent.futures import ThreadPoolExecutor # <--- IMPORT IMPORTANT
from dotenv import load_dotenv
from livekit.agents import AutoSubscribe, JobContext, WorkerOptions, cli
from livekit import rtc
from vosk import Model, KaldiRecognizer, SetLogLevel
from  map_reduce_processor import generate_report_simple
from nettoye_transcription import clean_transcription_rules,clean_text
from login import get_auth_token
from SemanticTranscriptChunker import generate_report
from report import Report


load_dotenv()
logging.basicConfig(level=logging.WARNING)
logger = logging.getLogger("ai-agent")
logger.setLevel(logging.INFO)


BACKEND_URL = "http://localhost:8089/"


agent_executor = ThreadPoolExecutor(max_workers=3)


SetLogLevel(-1)
MODEL_PATH = "model"
if not os.path.exists(MODEL_PATH):
    logger.error(f"❌ Le dossier '{MODEL_PATH}' est introuvable !")
    exit(1)

logger.info("📥 Chargement du modèle Vosk...")
vosk_model = Model(MODEL_PATH)
logger.info("✅ Modèle chargé !")

meeting_transcript = []


# --- FONCTIONS API ---


def get_meeting_context(meeting_id):
  
    url = f"{BACKEND_URL}meeting/{meeting_id}"
    logger.info(f"🌍 Fetching context depuis: {url}")
    token = get_auth_token()

    if not token:
        logger.error("❌ Impossible d'envoyer le rapport (token manquant)")
        return
        
    headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

    try:
        response = requests.get(url, headers=headers, timeout=5)
        
        # Vérification si le contenu est bien du JSON
        if "application/json" in response.headers.get("Content-Type", ""):
            data = response.json()
            if response.status_code == 200: 
                data = response.json()
                logger.info("✅ Données reçues du Backend !")

                normalized_context = {
                "id": data.get("meetId", "unknown_id"),
                "title": data.get("meetingTitle", "Réunion Sans Titre"),
                "date": data.get("meetingDate", "Date Inconnue"),       
                "duration": data.get("duration"),
                "sections": data.get("sections", []) 
            }
          
                print(f"DEBUG MAPPING: Titre='{normalized_context['title']}' | Sections={len(normalized_context['sections'])}")
            
                return normalized_context
               
            else:
                logger.warning(f"⚠️ Erreur Backend {response.status_code}: {data.get('error')}")
        else:
            logger.error(f"❌ Le backend n'a pas renvoyé de JSON. Reçu: {response.text[:100]}...")

    except Exception as e:
        logger.error(f"❌ Erreur critique connexion Backend : {e}")

    return {"title": "Réunion Inconnue (Erreur)", "sections": []}
    
   

def report_to_json(report,context):
    return {
        
        "meetId": context.get("id", "unknown_meet_id")  ,
        "templateId": context.get("templateId", "unknown_template_id"),
        "title": report.title,
        "sections": [
            {
                "code": s.code,
                "title": s.title,
                "content": s.content,
                "order": s.order,
                "aiConfidence": s.aiConfidence,
                "aiReviewHints": s.aiReviewHints
            }
            for s in report.sections
        ]
    }


def save_report_to_backend(report_data):
    try:
        token = get_auth_token()

        if not token:
            logger.error("❌ Impossible d'envoyer le rapport (token manquant)")
            return

        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }

        response = requests.post(f"{BACKEND_URL}api/reports/save_or_update", json=report_data,headers=headers, timeout=10)
        if response.status_code == 200:
            logger.info(f"🎉 Rapport sauvegardé ou mis à jour pour meetId: {report_data.get('meetId')}")
        else:
            logger.error(f"❌ Erreur sauvegarde: {response.status_code} - {response.text}")
    except Exception as e:
        logger.error(f"❌ Erreur critique lors de l'envoi : {e}")



# --- AUDIO PROCESSING ---
async def process_audio_vosk(track: rtc.AudioTrack, participant: rtc.RemoteParticipant):
    logger.info(f"🎤 Écoute de {participant.identity}")
    audio_stream = rtc.AudioStream(track)
    recognizer = KaldiRecognizer(vosk_model, 16000)
    recognizer.SetWords(True)
    
    buffer = bytearray()

    try:
        async for event in audio_stream:
            data_48k = np.frombuffer(event.frame.data, dtype=np.int16)
            data_16k = data_48k[::3]
            buffer.extend(data_16k.tobytes())

            if len(buffer) >= 4000:
                if recognizer.AcceptWaveform(bytes(buffer)):
                    result = json.loads(recognizer.Result())
                    text = result.get("text", "").strip()
                    if text:
                        log_entry = f"{participant.identity}: {text}"
                        print(f"📝 {log_entry}")
                        meeting_transcript.append(log_entry)
                buffer = bytearray()
    except Exception as e:
        logger.warning(f"Arrêt écoute {participant.identity}: {e}")

# --- ENTRYPOINT ---
async def entrypoint(ctx: JobContext):
    MEETING_ID = ctx.room.name
    logger.info(f"🤖 Agent connecté. Meeting ID : {MEETING_ID}")
    
    await ctx.connect(auto_subscribe=AutoSubscribe.AUDIO_ONLY)

    disconnect_event = asyncio.Event()

    @ctx.room.on("track_subscribed")
    def on_track_subscribed(track: rtc.Track, publication: rtc.TrackPublication, participant: rtc.RemoteParticipant):
        if track.kind == rtc.TrackKind.KIND_AUDIO:
            asyncio.create_task(process_audio_vosk(track, participant))

    @ctx.room.on("disconnected")
    def on_disconnected(reason):
        logger.info(f"🔌 Déconnexion : {reason}")
        disconnect_event.set()

    try:
        await disconnect_event.wait()
    except asyncio.CancelledError:
        pass
    except Exception as e:
        logger.error(f"Erreur inattendue : {e}")
    # ... (Tes imports, ajoute sys si absent)


    finally:
        logger.info("🛑 FIN DE RÉUNION. Lancement du workflow IA...")
        
        full_text = "\n".join(meeting_transcript)
        
        if len(full_text) > 10:
            print("\n🌍 1. Récupération contexte (Spring Boot)...")
            
            # APPEL DIRECT (Sans await, sans executor)
            # requests est bloquant, mais c'est parfait ici, on veut que le script attende !
            context = get_meeting_context(MEETING_ID)
            print("context -------")
            print(context)
            
            print(f"📋 Infos reçues. Titre: {context.get('title')}")

            # Construction du template
            template_from_backend = {
                "id": context.get("id"),
                "name": "Template Backend",
                "sections": context.get("sections", [])
            }
            
            if not template_from_backend["sections"]:
                print("⚠️ Pas de sections, utilisation défaut.")
                template_from_backend["sections"] = [
                    {"code": "SUMMARY", "title": "Résumé", "enabled": True, "guidance": "Synthèse."},
                    {"code": "ACTIONS", "title": "Actions", "enabled": True, "guidance": "Tâches."}
                ]

            print("\n🧠 2. Génération du rapport (LangChain)...")
            
            # --- MODIFICATION MAJEURE ICI ---
            # On appelle directement la fonction. 
            # Si ça bloque ici, c'est que c'est TA CONNEXION INTERNET ou CLÉ API qui bloque.
            try:
                
                transciption_nettoyee =  clean_transcription_rules(full_text)
                print(f"DEBUG : Transcription nettoyée (200 premiers chars) : {transciption_nettoyee[:200]}...")
                
                
                #report = generate_report(transciption_nettoyee, template_from_backend, context)
                report = generate_report(transciption_nettoyee, template_from_backend, context)
                

                print("\n💾 3. Sauvegarde (Spring Boot)...")
                payload = report_to_json(report,context)
                print("payload ---",payload)
                #payload["title"] = clean_text(payload.get("title", ""))
                #for section in payload.get("sections", []):
                   #section["title"] = clean_text(section.get("title", ""))
                   #section["content"] = clean_text(section.get("content", ""))
                   #if "aiReviewHints" in section:
                        #section["aiReviewHints"] = [clean_text(hint) for hint in section["aiReviewHints"]]


   
                print("rapport format JSON ----------------")
                print(payload)
                print("----------------------------")
                save_report_to_backend( payload)
                
                print("🏁 Workflow terminé avec succès.")
                
            except Exception as e:
                logger.error(f"❌ ERREUR LORS DE LA GÉNÉRATION : {e}")

        else:
            logger.warning("Discussion trop courte ou vide.")
            
        print("👋 Arrêt de l'agent.")
        # sys.exit(0) # Tu peux décommenter ça pour forcer l'arrêt si ça reste bloqué

if __name__ == "__main__":
    cli.run_app(WorkerOptions(entrypoint_fnc=entrypoint))