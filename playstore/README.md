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
• English, German, French, Italian, Spanish and Portuguese

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
• Em português, inglês, espanhol, alemão, francês e italiano

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
• En español, inglés, portugués, alemán, francés e italiano

PRIVADO DE VERDAD
Los nombres y las ideas de regalo quedan en tu teléfono. El resultado viaja dentro de cada enlace, y la página que lo abre nunca lo envía a un servidor.

Gratis, con anuncios.
```

> **es-419 — proposta de troca de termo (out/2026).** "Amigo invisible" é o termo
> da Espanha (e da Argentina/Uruguai). Na maior parte da América Latina busca-se
> "amigo secreto" (Colômbia, Chile, Peru, Venezuela) ou "intercambio de regalos"
> (México). Como a Espanha ainda está fora da distribuição, a ficha **es-419**
> rende mais com:
>
> - **Título:** `Amigo Secreto: Sorteo`
> - **Descripción breve:** `Sorteo de amigo secreto gratis. Sin correo ni registro. Sortea al instante.`
> - **Primeira frase da descrição:** `La forma más fácil de organizar el amigo secreto (también llamado intercambio de regalos o amigo invisible).`
>
> A ficha **es-ES** fica como está. Dentro do app nada muda.

### de-DE

**Titel** (30)
```
Wichteln: Namen auslosen
```

**Kurzbeschreibung** (80)
```
Wichteln kostenlos auslosen. Ohne E-Mail, ohne Anmeldung. In Sekunden fertig.
```

**Vollständige Beschreibung**
```
Die einfachste Art, das Wichteln (Secret Santa) zu organisieren. Ohne E-Mail-Adressen, ohne Konto, ohne Bezahlschranke – und nicht einmal wer organisiert, erfährt, wer wen gezogen hat.

SO FUNKTIONIERT ES
1. Erstelle eine Gruppe und füge alle hinzu. Die ganze Liste aus dem Gruppenchat lässt sich auf einmal einfügen.
2. Wenn du magst, trage ein Geschenkbudget und ein paar Geschenkideen für jede Person ein.
3. Lege fest, wer wen nicht ziehen darf: Paare, Geschwister, alle, die sich ohnehin beschenken.
4. Lose aus und wähle, wie alle ihr Ergebnis erfahren.

EIN PRIVATER LINK FÜR JEDEN
Schicke jeder Person ihren eigenen Link über eine beliebige Messenger-App. Sie tippt darauf, öffnet ihr Geschenk direkt im Browser und sieht, wen sie beschenkt – samt Geschenkideen und Budget. Niemand muss etwas installieren.

Der Bildschirm des Organisators zeigt nur, wem der Link schon geschickt wurde – nie die Paarungen. Endlich kann auch mitspielen, wer organisiert.

ODER DAS HANDY WEITERGEBEN
Alle im selben Raum? Jede Person tippt auf ihren eigenen Namen, sieht ihr Los ganz privat und gibt das Handy weiter.

EINE AUSLOSUNG, DIE IMMER AUFGEHT
Jeder schenkt genau einmal und wird genau einmal beschenkt. Niemand zieht sich selbst, niemand bleibt übrig, und deine Regeln werden immer eingehalten. Machen die Regeln eine Auslosung unmöglich, sagt die App es dir, statt zu raten.

FÜR ECHTE GRUPPEN GEMACHT
• Weihnachtsfeier im Büro, Wichteln in der Familie, im Verein oder im Freundeskreis
• Gruppen bleiben für nächstes Jahr gespeichert
• Aktuelle Links, wenn du die Gruppe nach der Auslosung änderst
• Auf Deutsch, Englisch, Französisch, Italienisch, Spanisch und Portugiesisch

PRIVAT VON GRUND AUF
Namen und Geschenkideen bleiben auf deinem Handy. Das Ergebnis steckt in jedem Link, und die Seite, die ihn öffnet, schickt es nie an einen Server.

Kostenlos, durch Werbung finanziert.
```

### fr-FR e fr-CA

**Titre** (30)
```
Père Noël Secret : Tirage
```

**Brève description** (80)
```
Tirage au sort de Père Noël secret gratuit. Sans e-mail, sans inscription.
```

**Description complète**
```
La façon la plus simple d’organiser un Père Noël secret (Secret Santa). Sans adresses e-mail, sans compte, sans rien payer — et même la personne qui organise ne sait pas qui a tiré qui.

COMMENT ÇA MARCHE
1. Crée un groupe et ajoute tout le monde. Tu peux coller d’un coup toute la liste de ta conversation de groupe.
2. Si tu veux, ajoute un budget cadeau et quelques idées cadeaux pour chacun.
3. Indique qui ne peut pas tirer qui : couples, frères et sœurs, ceux qui s’offrent déjà des cadeaux.
4. Lance le tirage au sort, puis choisis comment chacun découvre son résultat.

UN LIEN PRIVÉ POUR CHACUN
Envoie à chaque personne son propre lien par l’appli de messagerie de ton choix. Elle le touche, ouvre son cadeau directement dans le navigateur et voit à qui elle offre, avec les idées cadeaux et le budget. Personne n’a besoin d’installer quoi que ce soit.

L’écran de l’organisateur montre seulement à qui le lien a déjà été envoyé — jamais les binômes. Enfin, l’organisateur peut jouer lui aussi.

OU FAIS PASSER LE TÉLÉPHONE
Tout le monde dans la même pièce ? Chacun touche son prénom, découvre son résultat en privé et passe le téléphone au suivant.

UN TIRAGE QUI TOMBE TOUJOURS JUSTE
Chacun offre exactement un cadeau et en reçoit exactement un. Personne ne se tire soi-même, personne n’est oublié, et tes règles sont toujours respectées. Si tes règles rendent le tirage impossible, l’appli te le dit au lieu de deviner.

PENSÉ POUR DE VRAIS GROUPES
• Noël en famille, repas de fin d’année au bureau, échange de cadeaux entre amis
• Groupes enregistrés pour l’année prochaine
• Liens mis à jour si tu modifies le groupe après le tirage
• En français, anglais, allemand, italien, espagnol et portugais

PRIVÉ PAR CONCEPTION
Les prénoms et les idées cadeaux restent sur ton téléphone. Le résultat voyage dans chaque lien, et la page qui l’ouvre ne l’envoie jamais à un serveur.

Gratuit, financé par la publicité.
```

### it-IT

**Titolo** (30)
```
Babbo Natale Segreto: Estrai
```

**Descrizione breve** (80)
```
Estrazione Babbo Natale segreto gratis. Senza email, senza registrazione.
```

**Descrizione completa**
```
Il modo più semplice per organizzare il Babbo Natale segreto (Secret Santa). Senza indirizzi email, senza account, senza pagare nulla — e nemmeno chi organizza scopre chi ha pescato chi.

COME FUNZIONA
1. Crea un gruppo e aggiungi tutti. Puoi incollare in una volta l’intera lista dalla chat di gruppo.
2. Se vuoi, aggiungi un budget per il regalo e qualche idea regalo per ogni persona.
3. Indica chi non può pescare chi: coppie, fratelli, chi si scambia già i regali.
4. Estrai i nomi e scegli come ognuno scopre il risultato.

UN LINK PRIVATO PER OGNUNO
Invia a ogni persona il proprio link con l’app di messaggistica che preferisci. Lo tocca, apre il regalo direttamente nel browser e vede a chi deve fare il regalo, con le idee regalo e il budget. Nessuno deve installare nulla.

Lo schermo di chi organizza mostra solo a chi è già stato inviato il link — mai gli abbinamenti. Finalmente anche chi organizza può giocare.

OPPURE PASSA IL TELEFONO
Tutti nella stessa stanza? Ognuno tocca il proprio nome, vede in privato chi ha pescato e passa il telefono al prossimo.

UN’ESTRAZIONE CHE TORNA SEMPRE
Ognuno fa esattamente un regalo e ne riceve esattamente uno. Nessuno pesca se stesso, nessuno resta fuori, e le tue regole vengono sempre rispettate. Se le regole rendono impossibile l’estrazione, l’app te lo dice invece di tirare a indovinare.

FATTA PER GRUPPI VERI
• Natale in famiglia, festa in ufficio, scambio di regali tra amici
• Gruppi salvati per l’anno prossimo
• Link aggiornati se cambi il gruppo dopo l’estrazione
• In italiano, inglese, tedesco, francese, spagnolo e portoghese

PRIVATA PER COME È FATTA
I nomi e le idee regalo restano sul tuo telefono. Il risultato viaggia dentro ogni link, e la pagina che lo apre non lo invia mai a un server.

Gratis, con pubblicità.
```

> **Capturas de/fr/it.** Feitas em 09/10/2026 no Redmi, com o build de debug em
> cada idioma, e já enviadas às fichas de-DE, fr-CA, fr-FR e it-IT junto com os
> gráficos de destaque. A captura do presente em inglês (`shots-en/reveal.png`)
> foi refeita no mesmo dia: a anterior estava com o texto em português.
