import re

def clean_transcription_rules(raw_text):
    """
    Nettoie la transcription en fusionnant les segments consécutifs du même locuteur.
    Supprime l'emoji 📝 et ne répète pas le nom si c'est la même personne qui parle.
    """
    # On découpe le texte ligne par ligne
    lines = raw_text.strip().split('\n')
    
    cleaned_lines = []
    last_speaker = None

    # Regex pour capturer : (L'emoji) (Le Nom) : (Le Contenu)
    # Explication : 
    # 📝\s+   -> Cherche l'emoji note suivi d'espace
    # (.*?):  -> Capture le nom du locuteur jusqu'aux deux points
    # \s?(.*) -> Capture tout le reste (le texte)
    pattern = re.compile(r"📝\s+(.*?):\s?(.*)")

    for line in lines:
        line = line.strip()
        if not line: continue # Ignorer les lignes vides

        match = pattern.match(line)

        if match:
            current_speaker = match.group(1) # ex: hiba123@gmail.com
            content = match.group(2)         # ex: votre bouche autre langue...

            if current_speaker == last_speaker:
                # C'est le MÊME locuteur qui continue -> on met juste le texte
                cleaned_lines.append(content)
            else:
                # C'est un NOUVEAU locuteur (ou le premier) -> on met "Nom: Texte"
                cleaned_lines.append(f"{current_speaker}: {content}")
                last_speaker = current_speaker
        else:
            # Si la ligne ne correspond pas au format (pas de 📝), on la garde telle quelle
            cleaned_lines.append(line)

    # On rejoint tout avec des sauts de ligne
    return "\n".join(cleaned_lines)

def clean_text(text):
    if not isinstance(text, str):
        return text
    # Échapper les guillemets doubles
    text = text.replace('"', '\\"')
    # Supprimer les caractères invisibles bizarres (hors \n, \t)
    text = re.sub(r'[^\x20-\x7E\u00C0-\u017F\n\t\u202F]', '', text)
    return text


# --- TEST AVEC VOTRE EXEMPLE ---
input_text = """
📝 hiba123@gmail.com: votre bouche autre langue vos cordes vocales tout s'habitue au son du français seconde fois que vous gagnez en fluidité et surtout accepter 
le malaise oui au début vous allez vous sentir ridicule oui vous allez vous trompez oui il y aura
📝 hiba123@gmail.com: des moments de silence gênant mais c'est normal vous ne pouvez pas attendre d'être parfait avant de parler c'est en parlant vous allez devenir bon et ce qui est magique c'est qu'au bout d'un moment cette gêne va disparaître vous allez avoir cette petite révélation à enfaite je peux
📝 hiba123@gmail.com: peut parler français ce n'est pas si compliqué que ça je me souviens d'un élève qui était bloqués depuis des mois il connaissait parfaitement la grammaire les il n'osait pas parler un jour je lui ai dit ok aujourd'hui on 
va juste discuter tu peux faire toutes les erreurs que tu veux on s'en fiche      
📝 hiba123@gmail.com: il a commencé à parler il s'est trompé il a cherché ces mots mais il a continué et au bout de dix minutes
"""

print("--- RÉSULTAT ---")
print(clean_transcription_rules(input_text))