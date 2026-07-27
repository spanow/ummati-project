# syntax=docker/dockerfile:1

# Reverse proxy Ummati.
#
# La configuration est copiée dans l'image plutôt que montée depuis l'hôte : l'image
# devient autoportante (déployable telle quelle, sans dépendre de fichiers présents sur
# la machine cible) et on évite les problèmes de montage de fichier unique rencontrés
# sous Docker Desktop / WSL.
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

# 127.0.0.1 et non « localhost » : nginx n'écoute qu'en IPv4, alors que la résolution
# de localhost dans l'image Alpine tente ::1 en premier et échoue.
# GET et non --spider (HEAD) : le serveur SSR Angular ne répond pas aux requêtes HEAD.
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
