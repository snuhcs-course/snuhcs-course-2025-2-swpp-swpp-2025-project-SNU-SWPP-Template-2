# Use PostGIS 14 image (includes PostgreSQL 14)
FROM postgis/postgis:14-3.3

# Install locales and add Korean UTF-8
RUN apt-get update && \
    apt-get install -y locales && \
    localedef -i ko_KR -c -f UTF-8 -A /usr/share/locale/locale.alias ko_KR.UTF-8

# Set environment variables for Korean locale
ENV LANG=ko_KR.UTF-8
ENV LC_ALL=ko_KR.UTF-8
