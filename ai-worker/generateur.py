import re
import os
import json
import numpy as np
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
from langchain_groq import ChatGroq
from dotenv import load_dotenv
from report import Report, ReportSectionContent, RefinedSectionResponse

# ---------- Configuration ----------
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2')
load_dotenv()

def get_llm(temperature: float = 0.2, max_retries: int = 3) -> ChatGroq:
  
    api_key = os.getenv("GROQ_API_KEY")
    if not api_key:
        raise ValueError("GROQ_API_KEY manquante. Définissez-la dans les variables d'environnement.")
    
    return ChatGroq(
        model="openai/gpt-oss-120b",
        temperature=temperature,
        api_key=api_key,
        max_retries=max_retries,
        request_timeout=30
    )

# Instance LLM globale (réutilisable)
llm = get_llm()

# ---------- 1. Parsing des interventions ----------
def parse_transcript_to_interventions(text):
    speaker_pattern = re.compile(r'^([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}):\s*(.*)', re.IGNORECASE)
    lines = text.strip().split('\n')
    interventions = []
    current_speaker = None
    current_text = []
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        match = speaker_pattern.match(line)
        if match:
            if current_speaker and current_text:
                interventions.append({
                    "speaker": clean_speaker_name(current_speaker),
                    "text": " ".join(current_text).strip()
                })
            email = match.group(1)
            current_speaker = email
            text_start = match.group(2)
            current_text = [text_start] if text_start else []
        else:
            if current_speaker:
                current_text.append(line)
    
    if current_speaker and current_text:
        interventions.append({
            "speaker": clean_speaker_name(current_speaker),
            "text": " ".join(current_text).strip()
        })
    return interventions

def clean_speaker_name(email):
    local_part = email.split('@')[0]
    name = re.sub(r'[^a-zA-Z]', '', local_part)
    return name.capitalize() if name else local_part.capitalize()

# ---------- 2. Segmentation sémantique ----------
def segment_interventions(interventions, model):
    texts = [f"{i['speaker']}: {i['text']}" for i in interventions]
    embeddings = model.encode(texts)
    
    sims = [cosine_similarity([embeddings[i]], [embeddings[i+1]])[0][0] 
            for i in range(len(embeddings)-1)]
    
    threshold = np.mean(sims) - 0.5 * np.std(sims)
    boundaries = [i+1 for i, s in enumerate(sims) if s < threshold]
    
    segments = []
    start = 0
    for b in boundaries:
        segments.append(interventions[start:b])
        start = b
    segments.append(interventions[start:])
    return segments

# ---------- 3. Indexation des segments ----------
def index_segments(segments, model):
    segment_texts = [" ".join([f"{turn['speaker']}: {turn['text']}" for turn in seg]) 
                     for seg in segments]
    segment_embeddings = model.encode(segment_texts)
    return segment_texts, np.array(segment_embeddings)

# ---------- 4. Génération avec vérification ----------
def generate_section_with_verification(section, segments, segment_embeddings, model, llm, 
                                       top_k=3, confidence_threshold=70, max_iterations=3):
    """
    Génère le contenu d'une section, l'évalue, et l'améliore itérativement.
    Retourne un objet RefinedSectionResponse.
    """
    # 1. Récupérer les segments pertinents
    query = f"{section['title']} : {section['guidance']}"
    query_emb = model.encode([query])[0]
    sims = cosine_similarity([query_emb], segment_embeddings)[0]
    top_indices = np.argsort(sims)[::-1][:top_k]
    
    context_parts = []
    for idx in top_indices:
        for turn in segments[idx]:
            context_parts.append(f"{turn['speaker']}: {turn['text']}")
    context = "\n".join(context_parts)

    # 2. Premier jet
    draft_prompt = f"""Tu es un assistant qui rédige des rapports de réunion.

Section à rédiger : {section['title']}
Consigne : {section['guidance']}

Extraits de la conversation :
{context}

Rédige un premier jet pour cette section, uniquement le contenu, sans titre."""
    
    draft_response = llm.invoke(draft_prompt)
    draft_content = draft_response.content

    # 3. Évaluation du brouillon
    def evaluate_content(content):
        critique_prompt = f"""Tu es un relecteur exigeant de rapports de réunion.

Voici une section de rapport générée automatiquement :
"{content}"

Consigne attendue pour cette section : {section['guidance']}

Extraits de la conversation source :
{context}

Analyse cette section et réponds UNIQUEMENT au format JSON suivant :
{{
    "critique": "explique ce qui va bien et ce qui manque, les éventuelles hallucinations",
    "has_info": true ou false (la section contient-elle de vraies informations issues de la conversation ?),
    "confidence_score": un nombre entre 1 et 100 (confiance dans la pertinence / factualité)
}}

JSON uniquement :"""
        response = llm.invoke(critique_prompt)
        try:
            data = json.loads(response.content)
            critique = data.get("critique", "")
            has_info = data.get("has_info", False)
            confidence = data.get("confidence_score", 0)
        except:
            critique = response.content
            has_info = True
            confidence = 50
        return critique, has_info, confidence

    critique, has_info, confidence = evaluate_content(draft_content)
    final_content = draft_content

    # 4. Boucle d'amélioration (max_iterations)
    iteration = 0
    while (confidence < confidence_threshold or not has_info) and iteration < max_iterations:
        improved_prompt = f"""Tu es un assistant qui rédige des rapports de réunion.

Section à rédiger : {section['title']}
Consigne : {section['guidance']}

Tu as déjà produit un brouillon, mais il a été critiqué ainsi :
{critique}

Améliore le texte en tenant compte de cette critique. 
Utilise uniquement les extraits suivants de la conversation :
{context}

Rédige la version améliorée de la section."""
        
        improved_response = llm.invoke(improved_prompt)
        final_content = improved_response.content
        
        # Ré-évaluer
        critique, has_info, confidence = evaluate_content(final_content)
        iteration += 1

    return RefinedSectionResponse(
        draft_content=draft_content,
        critique=critique,
        final_content=final_content,
        has_info=has_info,
        confidence_score=confidence
    )

# ---------- 5. Génération complète du rapport ----------
def generate_report(transcript, template, context):
    # Filtrer les sections activées une seule fois
    sections = [s for s in template.get("sections", []) if s.get("enabled", True)]
    
    interventions = parse_transcript_to_interventions(transcript)
    segments = segment_interventions(interventions, model)
    _, segment_embeddings = index_segments(segments, model)

    result_sections = []
    for sec in sections:
        verified = generate_section_with_verification(
            sec, segments, segment_embeddings, model, llm
        )
        section = ReportSectionContent(
            code=sec["code"],
            title=sec["title"],
            content=verified.final_content,
            order=sec.get("order", 0),
            aiConfidence=str(verified.confidence_score),
            aiReviewHints=[verified.critique] if verified.critique else []
        )
        result_sections.append(section)

    report = Report(
        id=context.get("reportId", "unknown"),
        meetId=context.get("meetId", "unknown"),
        templateId=context.get("templateId", "unknown"),
        title=context.get("title", "Rapport de réunion"),
        sections=result_sections
    )
    return report

def test_generate_report():
    """Fonction de test du pipeline complet de génération de rapport."""
    
    # 1. Transcription fictive (avec locuteurs au format email)
    transcript = """hiba123@gmail.com: votre bouche autre langue vos cordes vocales tout s'habitue au son du français seconde fois que vous gagnez en fluidité et surtout accepter
le malaise oui au début vous allez vous sentir ridicule oui vous allez vous trompez oui il y aura
des moments de silence gênant mais c'est normal vous ne pouvez pas attendre d'être parfait avant de parler c'est en parlant vous allez devenir bon et ce qui est magique c'est qu'au bout d'un moment cette gêne va disparaître vous allez avoir cette petite révélation à enfaite je peux
peut parler français ce n'est pas si compliqué que ça je me souviens d'un élève qui était bloqués depuis des mois il 
connaissait parfaitement la grammaire les il n'osait pas parler un jour je lui ai dit ok aujourd'hui on
va juste discuter tu peux faire toutes les erreurs que tu veux on s'en fiche
il a commencé à parler il s'est trompé il a cherché ces mots mais il a continué et au bout de dix minutes

pierre_dupont@domain.com: Je suis d'accord avec Hiba, la pratique est essentielle.
Il faut vraiment insister sur l'aspect psychologique.
"""

    # 2. Template de rapport (sections activées)
    template = {
        "sections": [
            {
                "code": "OBJECTIVES",
                "title": "Objectifs de la réunion",
                "guidance": "Identifie les objectifs principaux discutés.",
                "enabled": True,
                "order": 1
            },
            {
                "code": "DECISIONS",
                "title": "Décisions prises",
                "guidance": "Liste les décisions qui ont été validées.",
                "enabled": True,
                "order": 2
            },
            {
                "code": "ACTIONS",
                "title": "Actions à mener",
                "guidance": "Détaille les actions, les responsables et les échéances.",
                "enabled": False,   # désactivée pour tester le filtrage
                "order": 3
            }
        ]
    }

    # 3. Contexte (métadonnées)
    context = {
        "reportId": "RPT-2025-001",
        "meetId": "MEET-123",
        "templateId": "TMPL-FR-01",
        "title": "Compte rendu - Atelier français"
    }

    print("🚀 Lancement de la génération du rapport de test...")
    try:
        report = generate_report(transcript, template, context)
    except ValueError as e:
        print(f"❌ Erreur : {e}")
        print("💡 Vérifie que la variable d'environnement GROQ_API_KEY est définie.")
        return

    # 4. Affichage du rapport complet en JSON
    print("\n📄 Rapport généré :")
    print(report.model_dump_json(indent=2, exclude_none=True))

    # 5. Assertions simples pour vérifier la structure
    assert report.id == "RPT-2025-001"
    assert report.meetId == "MEET-123"
    assert report.templateId == "TMPL-FR-01"
    assert report.title == "Compte rendu - Atelier français"
    assert len(report.sections) == 2  # seule les 2 premières sont activées
    for section in report.sections:
        assert section.code in ["OBJECTIVES", "DECISIONS"]
        assert section.content.strip() != ""
        assert 0 <= int(section.aiConfidence) <= 100
        assert isinstance(section.aiReviewHints, list)
    
    print("\n✅ Tous les tests structurels ont réussi !")
    print(f"📊 Scores de confiance : {[s.aiConfidence for s in report.sections]}")
    print(f"💬 Critiques : {[s.aiReviewHints for s in report.sections]}")

if __name__ == "__main__":
    # Exécute le test si le script est lancé directement
    test_generate_report()