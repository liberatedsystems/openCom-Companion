FROM ubuntu:24.04 as build

# Install dependencies
RUN DEBIAN_FRONTEND=noninteractive \
  apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y git libffi-dev python3-dev python3-virtualenv libssl-dev autoconf openjdk-17-jdk cmake libtool libssl-dev libncurses5-dev libsqlite3-dev libreadline-dev libtk8.6 libgdm-dev libpcap-dev unzip zip wget apksigner build-essential libopus-dev libogg-dev patchelf \
  && apt-get install --reinstall python3 

WORKDIR "/root"

# Copy Sideband
COPY ./ Sideband/

# Copy gradle init file
COPY  docker/init.gradle .gradle/

# Clone required repos
RUN git clone https://github.com/jacobeva/Reticulum \
    && git clone https://github.com/markqvist/LXMF \
    && git clone https://github.com/markqvist/LXST

# Switch branches
WORKDIR "Reticulum"
#RUN git switch ble-dev

WORKDIR ".."

# Fetch files required for sideband local repository

RUN git clone https://github.com/liamcottle/rnode-flasher.git

RUN mkdir dist_archive 

WORKDIR "dist_archive" 

# Get latest version of whl packages from PyPI
COPY  docker/dist_env .

RUN export PACKAGENAME=rns; . ./dist_env; wget --content-disposition -q ${DL_URL}; \
    export PACKAGENAME=rnspure; . ./dist_env; wget --content-disposition -q ${DL_URL}; \
    export PACKAGENAME=lxmf; . ./dist_env; wget --content-disposition -q ${DL_URL}; \
    export PACKAGENAME=nomadnet; . ./dist_env; wget --content-disposition -q ${DL_URL}; \
    export PACKAGENAME=rnsh; . ./dist_env;  wget --content-disposition -q ${DL_URL}; \
    export PACKAGENAME=sbapp; . ./dist_env;  wget --content-disposition -q ${DL_URL}; 

# Get latest RNode Firmware release
RUN export VERSION=$(wget -qO - https://api.github.com/repos/markqvist/RNode_Firmware/releases/latest | grep "zipball_url" | grep -oP "\d+\.\d+"); \ 
    wget -qO - https://api.github.com/repos/markqvist/RNode_Firmware/releases/latest \
    | grep "zipball_url" | cut -d : -f 2,3 \
    | tr -d \", | wget -qi - -O RNode_Firmware_${VERSION}_Source.zip 

# Get source for reticulum.network and unsigned.io sites
RUN wget -q https://github.com/markqvist/reticulum_website/archive/refs/heads/main.zip \
    && unzip main.zip "reticulum_website-main/docs/*" \
    && cp -r reticulum_website-main/docs reticulum.network && rm -r reticulum_website-main \
    && cp reticulum.network/manual/Reticulum\ Manual.pdf . \
    && cp reticulum.network/manual/Reticulum\ Manual.epub . 

# A mirror can also be accessed at https://liberatedsystems.co.uk/unsigned_io_archive.zip if unsigned.io is down!
RUN wget -q https://liberatedsystems.co.uk/unsigned_io_archive.zip \ 
    && unzip -q unsigned_io_archive.zip && rm unsigned_io_archive.zip 


WORKDIR "../Sideband/sbapp"

# Set up virtual environment
RUN virtualenv venv 

RUN bash -c "source venv/bin/activate && pip install -U pip && pip install setuptools==74.1.2 wheel==0.43 buildozer==1.4.0 cython==3.0.10 rich==13.8.1"

WORKDIR "../"

RUN bash -c "make release"

FROM scratch as artifact
COPY --from=build /root/Sideband/dist/* /
