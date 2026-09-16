# Publicação — Secret Santa / Amigo Secreto

Tudo o que a Play Console pede, pronto para colar. Os passos estão na ordem que
evita retrabalho; os que dependem de conta (GitHub, Squarespace, AdMob, Play)
são seus, o resto já está no repositório.

## Arquivos prontos para upload

| Arquivo | Tamanho | Onde vai |
| --- | --- | --- |
| `icon-512.png` | 512×512 | Ícone do app (a Play arredonda sozinha) |
| `feature-graphic-en.png` | 1024×500 | Gráfico de destaque **en-US** |
| `feature-graphic-pt.png` | 1024×500 | Gráfico de destaque **pt-BR** |
| `feature-graphic-es.png` | 1024×500 | Gráfico de destaque **es-419 / es-ES** |
| `screenshots-en/01…06.png` | 1080×1920 | Capturas **en-US** |
| `screenshots-pt/01…06.png` | 1080×1920 | Capturas **pt-BR** |
| `screenshots-es/01…06.png` | 1080×1920 | Capturas **es-419 / es-ES** |
| `data_safety.csv` | — | Segurança dos dados → Importar CSV |

As capturas são reais, feitas no Redmi Note 9S. A barra de status, a barra do
Chrome e o banner de anúncio de teste foram recortados — prática permitida.
Para regerar: `bash playstore/render.sh`.

---

## Passo 1 — Site (GitHub Pages + DNS)

Sem isto nenhum link enviado pelo app abre. Faça primeiro: o DNS e o certificado
HTTPS podem levar de minutos a algumas horas.

1. **GitHub → New repository**: dono `borzanr`, nome `secretsanta-site`,
   **Public**, **sem** README (o repositório local já tem um).
2. Me avise que eu faço o push de `D:\Android\AndroidStudioProjects\secretsanta-site`
   — ou rode você:
   ```bash
   cd /d/Android/AndroidStudioProjects/secretsanta-site && git remote add origin https://github.com/borzanr/secretsanta-site.git && git push -u origin main
   ```
3. **Settings → Pages**: Source `Deploy from a branch`, branch `main`, pasta `/ (root)`.
   O campo Custom domain já vem preenchido pelo arquivo `CNAME`.
4. **Squarespace → Domínios → borzanti.com → DNS → Adicionar registro**:

   | Host | Tipo | Dados |
   | --- | --- | --- |
   | `secretsanta` | CNAME | `borzanr.github.io` |

   É o mesmo formato do `luckydraw` que já existe aí.
5. Volte em **Settings → Pages** e marque **Enforce HTTPS** quando liberar.
6. Teste em janela anônima: `https://secretsanta.borzanti.com/privacy/` e
   `https://secretsanta.borzanti.com/app-ads.txt`.

## Passo 2 — AdMob ✅

Feito em 16/09/2026. App `Secret Santa` na conta `pub-5043299631567488`:

| | ID |
| --- | --- |
| App ID | `ca-app-pub-5043299631567488~2456611633` |
| Banner (`secretsanta_banner`) | `ca-app-pub-5043299631567488/9885518823` |
| Intersticial (`secretsanta_interstitial`) | `ca-app-pub-5043299631567488/5946273810` |

Já aplicados no `AndroidManifest.xml` e em `ads/AdIds.kt`. Falta só, **depois**
de publicar na Play: **Configurações do app → Vincular à loja**.

## Passo 3 — Chave de upload

Rode você, no terminal — a senha é digitada no prompt e não passa por mim:

```bash
"/d/Program Files/Android/Android Studio/jbr/bin/keytool.exe" -genkeypair -v -keystore ~/keystores/secretsanta-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Depois crie `D:\Android\AndroidStudioProjects\SecretSanta\keystore.properties`
(já está no `.gitignore`) seguindo `keystore.properties.example`, com
`storeFile` apontando para `C:/Users/Rafael/keystores/secretsanta-upload.jks` e
`keyAlias=upload`. **Guarde a senha num gerenciador**: sem ela não sai atualização.

Com isso eu gero o AAB assinado: `app/build/outputs/bundle/release/app-release.aab`.

---

## Passo 4 — Play Console

### Criar o app
**Todos os apps → Criar app.** Nome `Secret Santa: Draw Names`, idioma padrão
**Inglês (Estados Unidos)**, **App**, **Gratuito**. O idioma padrão não muda
depois, e o mercado principal definido é o americano.

### Configuração do app

| Item | Resposta |
| --- | --- |
| Política de privacidade | `https://secretsanta.borzanti.com/privacy/` |
| Acesso ao app | Todo o conteúdo disponível sem restrições (não há login) |
| Anúncios | **Sim, contém anúncios** |
| Público-alvo | **13–15, 16–17 e 18+**. Não marcar faixas abaixo de 13: o app tem anúncios personalizados e entraria na política de Famílias |
| Apela a crianças? | Não |
| App de notícias | Não |
| Recursos do governo | Não |
| Recursos financeiros | Nenhum |
| Saúde | Não |
| ID de publicidade | **Sim**, usado para **Publicidade ou marketing** (vem do SDK do AdMob) |
| Categoria | **Estilo de vida** (Lifestyle) — onde estão os concorrentes diretos |
| E-mail de contato | `admin@borzanti.com` |
| Site | `https://secretsanta.borzanti.com` — é aqui que o AdMob procura o `app-ads.txt` |

### Classificação de conteúdo
Categoria **Utilitário, produtividade, comunicação ou outro**. Tudo **Não**,
inclusive:
- **Jogos de azar / apostas**: não. "Sorteio" aqui é de amigo secreto — não há
  dinheiro, prêmio, aposta nem simulação de cassino.
- **Usuários interagem / trocam conteúdo**: não. O app não tem chat nem conta; o
  link é enviado pelo app de mensagens que a pessoa escolher, fora do app.
- **Compartilha localização**: não.

Resultado esperado: Livre / Everyone / PEGI 3.

### Segurança dos dados
**Importar CSV** → `playstore/data_safety.csv`. Confere com a política:
- Coleta: **ID do dispositivo** (ID de publicidade), coletado e compartilhado
  com o Google para **publicidade**, obrigatório, criptografado em trânsito.
- Contas: nenhuma. Exclusão: sim, pela URL da política.
- Nomes, sugestões e grupos **não** entram: nunca saem do aparelho. O envio do
  link é iniciado pela pessoa pelo seletor do sistema, o que a Play não conta
  como coleta nem compartilhamento pelo app.

### Países
**Todos os países.** Três idiomas, inglês como fallback, sem restrição legal.

### Primeira versão
1. **Testes → Teste interno → Criar versão** → aceite o **Play App Signing** →
   envie o `app-release.aab`. Adicione seu e-mail como testador.
2. Instale pelo link de teste no Redmi, confira que abre, que o banner aparece e
   que um link enviado abre em `secretsanta.borzanti.com`.
3. **Produção → Criar versão → Promover** a do teste interno. Conta de organização
   não passa pela regra dos 12 testadores por 14 dias.

### Antes de enviar para revisão
- [ ] `https://secretsanta.borzanti.com/privacy/` abre em janela anônima
- [x] IDs reais do AdMob no AAB
- [ ] `versionCode 1`, `versionName 1.0`
- [ ] Classificação de conteúdo sem jogos de azar
- [ ] Data Safety importado

---

## Fichas da loja

A Play indexa título, descrição curta e descrição longa. Nenhum texto cita marca
de terceiros (WhatsApp etc.) — é motivo comum de reprovação de metadados.

### en-US (padrão)

**Title** (30)
```
Secret Santa: Draw Names
```

**Short description** (80)
```
Free Secret Santa generator. No email, no signup. Draw names in seconds.
```

**Full description**
```
The easiest way to run a Secret Santa gift exchange. No email addresses, no accounts, no paywall — and not even the organizer finds out who got whom.

HOW IT WORKS
1. Create a group and add everyone. Paste the whole list from your group chat in one go.
2. Add a gift budget and a few gift ideas for each person, if you like.
3. Mark who can't draw whom — couples, siblings, anyone who already exchanges gifts.
4. Draw names, then choose how everyone finds out.

A PRIVATE LINK FOR EVERYONE
Send each person their own link through any messaging app. They tap it, open their gift right in the browser and see who they're buying for, plus that person's gift ideas and the budget. Nobody needs to install anything.

The organizer's screen only shows who has been sent their link — never the matches. Finally, the organizer gets to play too.

OR PASS THE PHONE
Everyone in the same room? Each person taps their own name, sees their match in private and hands the phone to the next.

A DRAW THAT ALWAYS ADDS UP
Everyone gives exactly one gift and receives exactly one. Nobody draws themselves, nobody is left out, and your rules are always respected. If your rules make a draw impossible, the app tells you instead of guessing.

MADE FOR REAL GROUPS
• Office parties, family Christmas and holiday gift exchanges with friends
• Groups saved for next year
• Updated links if you change the group after drawing
• English, Portuguese and Spanish

PRIVATE BY DESIGN
Names and gift ideas stay on your phone. The result travels inside each link, and the page that opens it never sends it to a server.

Free, supported by ads.
```

### pt-BR

**Título** (30)
```
Amigo Secreto: Sorteio Fácil
```

**Descrição curta** (80)
```
Sorteio de amigo secreto grátis. Sem e-mail, sem cadastro. Sorteie na hora.
```

**Descrição completa**
```
O jeito mais fácil de organizar o amigo secreto. Sem e-mail, sem cadastro, sem pagar nada — e nem quem organiza fica sabendo quem tirou quem.

COMO FUNCIONA
1. Crie o grupo e adicione todo mundo. Dá para colar a lista inteira do grupo de mensagens de uma vez.
2. Se quiser, coloque o valor do presente e algumas sugestões para cada pessoa.
3. Marque quem não pode tirar quem: casais, irmãos, quem já troca presente de qualquer jeito.
4. Sorteie e escolha como cada um vai descobrir.

UM LINK SECRETO PARA CADA UM
Mande para cada pessoa o próprio link pelo app de mensagem que preferir. Ela toca, abre o presente direto no navegador e vê quem tirou, com as sugestões de presente e o valor. Ninguém precisa instalar nada.

A tela de quem organiza mostra só para quem já enviou o link — nunca os pares. Finalmente o organizador também participa.

OU PASSE O CELULAR
Todo mundo junto na confraternização? Cada um toca no próprio nome, vê quem tirou em particular e passa o celular para o próximo.

UM SORTEIO QUE SEMPRE FECHA A CONTA
Todo mundo dá exatamente um presente e recebe exatamente um. Ninguém tira a si mesmo, ninguém fica de fora, e as regras são sempre respeitadas. Se as regras tornarem o sorteio impossível, o app avisa em vez de chutar.

FEITO PARA GRUPOS DE VERDADE
• Amigo secreto da família, do trabalho, da turma, amigo oculto de Natal
• Grupos salvos para o ano que vem
• Links atualizados se você mudar o grupo depois do sorteio
• Em português, inglês e espanhol

PRIVADO DE VERDADE
Os nomes e as sugestões ficam no seu celular. O resultado viaja dentro de cada link, e a página que abre o link nunca envia o resultado para um servidor.

Grátis, mantido por anúncios.
```

### es-419 e es-ES

**Título** (30)
```
Amigo Invisible: Sorteo
```

**Descripción breve** (80)
```
Sorteo de amigo invisible gratis. Sin correo ni registro. Sortea al instante.
```

**Descripción completa**
```
La forma más fácil de organizar el amigo invisible (o amigo secreto). Sin correos, sin cuentas, sin pagar — y ni quien organiza se entera de a quién le tocó cada uno.

CÓMO FUNCIONA
1. Crea el grupo y agrega a todos. Puedes pegar la lista completa del chat del grupo de una vez.
2. Si quieres, agrega el valor del regalo y algunas ideas para cada persona.
3. Marca quién no puede sacar a quién: parejas, hermanos, quienes ya se regalan igual.
4. Sortea y elige cómo se entera cada uno.

UN ENLACE PRIVADO PARA CADA UNO
Envía a cada persona su propio enlace por la app de mensajes que prefieras. Lo toca, abre su regalo en el navegador y ve a quién le tocó, con sus ideas de regalo y el valor. Nadie necesita instalar nada.

La pantalla de quien organiza solo muestra a quién ya se le envió su enlace, nunca las parejas. Por fin, quien organiza también juega.

O PASA EL TELÉFONO
¿Todos en la misma fiesta? Cada persona toca su nombre, ve a quién le tocó en privado y pasa el teléfono al siguiente.

UN SORTEO QUE SIEMPRE CUADRA
Cada persona da exactamente un regalo y recibe exactamente uno. Nadie se saca a sí mismo, nadie queda afuera y tus reglas siempre se respetan. Si las reglas hacen imposible el sorteo, la app te avisa en lugar de adivinar.

HECHO PARA GRUPOS DE VERDAD
• Navidad en familia, fiesta de la oficina, intercambio de regalos con amigos
• Grupos guardados para el año que viene
• Enlaces actualizados si cambias el grupo después del sorteo
• En español, inglés y portugués

PRIVADO DE VERDAD
Los nombres y las ideas de regalo quedan en tu teléfono. El resultado viaja dentro de cada enlace, y la página que lo abre nunca lo envía a un servidor.

Gratis, con anuncios.
```
