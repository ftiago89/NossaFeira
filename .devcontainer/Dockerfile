# ============================================================
# Claude Code + Android Development Container
# Compatível com Apple Silicon (ARM64) e x86_64
# ============================================================

FROM node:22-bookworm-slim

# ── Argumentos de versão ─────────────────────────────────────
ARG JAVA_VERSION=17
ARG ANDROID_SDK_VERSION=11076708
ARG ANDROID_BUILD_TOOLS_VERSION=34.0.0
ARG ANDROID_PLATFORM_VERSION=34

# ── Variáveis de ambiente ────────────────────────────────────
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk \
    GRADLE_USER_HOME=/home/node/.gradle \
    NODE_OPTIONS="--max-old-space-size=4096" \
    CLAUDE_CONFIG_DIR=/home/node/.claude \
    DEBIAN_FRONTEND=noninteractive

# ── Dependências do sistema ──────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    curl \
    wget \
    unzip \
    zip \
    zsh \
    sudo \
    gnupg \
    ca-certificates \
    bash-completion \
    openjdk-${JAVA_VERSION}-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

# ── Android SDK (command-line tools) ────────────────────────
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd /tmp && \
    wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip" \
         -O cmdline-tools.zip && \
    unzip -q cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm cmdline-tools.zip && \
    chown -R node:node ${ANDROID_HOME}

# ── Aceitar licenças e instalar componentes Android ─────────
# Detecta o caminho do Java automaticamente (ARM64 e x86)
RUN JAVA_BIN=$(dirname $(readlink -f $(which java))) && \
    export PATH="${JAVA_BIN}:${ANDROID_HOME}/cmdline-tools/latest/bin:${PATH}" && \
    yes | sdkmanager --licenses && \
    sdkmanager --install \
        "platform-tools" \
        "platforms;android-${ANDROID_PLATFORM_VERSION}" \
        "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" \
        "cmdline-tools;latest" && \
    sdkmanager --update && \
    chown -R node:node ${ANDROID_HOME}

# ── Instalar Claude Code como usuário node ───────────────────
USER node
RUN curl -fsSL https://claude.ai/install.sh | bash

# ── Oh My Zsh ───────────────────────────────────────────────
RUN sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)" "" --unattended

# ── PATH permanente no shell ─────────────────────────────────
RUN echo 'export PATH="$HOME/.local/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH"' >> ~/.zshrc && \
    echo 'export PATH="$HOME/.local/bin:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:$PATH"' >> ~/.bashrc && \
    echo 'export ANDROID_HOME=/opt/android-sdk' >> ~/.zshrc && \
    echo 'export ANDROID_HOME=/opt/android-sdk' >> ~/.bashrc && \
    echo 'export GRADLE_USER_HOME=/home/node/.gradle' >> ~/.zshrc && \
    echo 'export GRADLE_USER_HOME=/home/node/.gradle' >> ~/.bashrc

# ── Criar diretórios necessários ────────────────────────────
USER root
RUN mkdir -p /home/node/.claude \
             /home/node/.gradle \
             /commandhistory \
             /workspace && \
    chown -R node:node \
        /home/node/.claude \
        /home/node/.gradle \
        /home/node/.oh-my-zsh \
        /commandhistory \
        /workspace

WORKDIR /workspace
USER node

CMD ["/bin/zsh"]
