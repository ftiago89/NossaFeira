#!/bin/bash
# ============================================================
# init-firewall.sh
# Adaptado do devcontainer oficial da Anthropic
# Restringe saída de rede apenas para domínios necessários:
#   - API do Claude (Anthropic)
#   - npm registry
#   - GitHub
#   - Google (Android SDK, Maven)
#   - JetBrains / Gradle repositories
# ============================================================

set -e

echo "[firewall] Configurando regras de rede..."

# ── Flush de regras existentes ───────────────────────────────
iptables -F OUTPUT 2>/dev/null || true
ip6tables -F OUTPUT 2>/dev/null || true

# ── Permitir loopback ────────────────────────────────────────
iptables -A OUTPUT -o lo -j ACCEPT
ip6tables -A OUTPUT -o lo -j ACCEPT

# ── Permitir conexões já estabelecidas ───────────────────────
iptables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
ip6tables -A OUTPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# ── Resolver IPs dos domínios permitidos e liberar ──────────
ALLOWED_DOMAINS=(
  # Anthropic / Claude
  "api.anthropic.com"
  "claude.ai"
  "statsig.anthropic.com"

  # npm
  "registry.npmjs.org"
  "npmjs.org"

  # GitHub
  "github.com"
  "api.github.com"
  "raw.githubusercontent.com"
  "objects.githubusercontent.com"

  # Google (Android SDK, Maven)
  "dl.google.com"
  "maven.google.com"
  "dl-ssl.google.com"

  # Gradle / JetBrains
  "plugins.gradle.org"
  "services.gradle.org"
  "downloads.gradle.org"
  "repo1.maven.org"
  "repo.maven.apache.org"
  "jcenter.bintray.com"
  "maven.pkg.jetbrains.space"
)

for DOMAIN in "${ALLOWED_DOMAINS[@]}"; do
  IPS=$(dig +short "$DOMAIN" 2>/dev/null | grep -E '^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$' || true)
  for IP in $IPS; do
    iptables -A OUTPUT -d "$IP" -j ACCEPT 2>/dev/null || true
  done
done

# ── Permitir DNS (porta 53) ──────────────────────────────────
iptables -A OUTPUT -p udp --dport 53 -j ACCEPT
iptables -A OUTPUT -p tcp --dport 53 -j ACCEPT
ip6tables -A OUTPUT -p udp --dport 53 -j ACCEPT
ip6tables -A OUTPUT -p tcp --dport 53 -j ACCEPT

# ── BLOQUEAR todo o resto ────────────────────────────────────
iptables -A OUTPUT -j DROP
ip6tables -A OUTPUT -j DROP

echo "[firewall] Regras aplicadas. Saída restrita aos domínios permitidos."
echo "[firewall] Domínios liberados: ${#ALLOWED_DOMAINS[@]}"
