FROM mcr.microsoft.com/devcontainers/java:25-bookworm

RUN rm -f /etc/apt/sources.list.d/yarn.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        bubblewrap \
        curl \
        git \
        jq \
        ripgrep \
        fd-find \
        python3 \
        python3-pip \
        python3-venv \
        pipx \
        nodejs \
        npm \
        build-essential \
        pkg-config \
        procps \
        gawk \
    && ln -sf /usr/bin/fdfind /usr/local/bin/fd \
    && rm -rf /var/lib/apt/lists/*

USER vscode

WORKDIR /workspace