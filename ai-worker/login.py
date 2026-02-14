import os
import requests
import logging
from dotenv import load_dotenv

logger = logging.getLogger(__name__)
load_dotenv()

BACKEND_URL = os.getenv("BACKEND_URL")
EMAIL = os.getenv("BACKEND_EMAIL")
PASSWORD = os.getenv("BACKEND_PASSWORD")
print(
    f"DEBUG LOGIN: BACKEND_URL={BACKEND_URL} | EMAIL={EMAIL} | PASSWORD={'*' * len(PASSWORD) if PASSWORD else None}"
)

_token_cache = None  


def get_auth_token():
    global _token_cache
    print(
    f"DEBUG LOGIN: BACKEND_URL={BACKEND_URL} | EMAIL={EMAIL} | PASSWORD={'*' * len(PASSWORD) if PASSWORD else None}"
)

  
    if _token_cache:
        return _token_cache

    try:
        response = requests.post(
            f"http://localhost:8089/auth/login",
            json={
                "email": EMAIL,
                "password": PASSWORD
            },
            timeout=10
        )

        if response.status_code == 200:
            _token_cache = response.json().get("token")
            logger.info(" Token JWT récupéré avec succès")
            return _token_cache
        else:
            logger.error(f" Login échoué: {response.status_code} - {response.text}")
            return None

    except Exception as e:
        logger.error(f" Erreur login backend: {e}")
        return None
