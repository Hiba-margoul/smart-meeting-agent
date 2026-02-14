from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any

class ReportSectionContent(BaseModel):
    code: str
    title: str
    content: str
    order: int
    aiConfidence: str = Field(default="N/A")
    aiReviewHints: List[str] = Field(default_factory=list)

class Report(BaseModel):
    id: str
    meetId: str
    templateId: str
    title: str
    sections: List[ReportSectionContent] = Field(default_factory=list)
    # Optionnel: ajouter des stats si besoin
    processing_stats: Optional[Dict[str, Any]] = None

class RefinedSectionResponse(BaseModel):
    draft_content: str = Field(description="Premier jet du contenu généré.")
    critique: str = Field(description="Critique de l'IA sur son propre brouillon.")
    final_content: str = Field(description="Version finale améliorée et corrigée.")
    has_info: bool = Field(description="True si la section contient de vraies infos.")
    confidence_score: int = Field(description="Score de confiance de 1 à 100.")