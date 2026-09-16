# Secret Santa / Amigo Secreto

Sorteio de amigo secreto offline, sem cadastro e sem e-mail. Terceiro app da
família `com.borzanti.*`, depois do LuckyDraw e do ScoreKeeper — a base de
build, o módulo de anúncios e a estrutura de tema vieram do LuckyDraw, no mesmo
caminho que gerou o ScoreKeeper.

| | |
|---|---|
| `applicationId` | `com.borzanti.secretsanta` |
| Pacote de código | `com.secretsanta.app` |
| Versão | 1.0 (`versionCode` 1) |
| Alvo de publicação | **~10 de outubro de 2026** (antes do pico de novembro/dezembro) |

## O que diferencia este app

A lista abaixo saiu das avaliações negativas dos concorrentes, não de um
brainstorming:

- **Sem e-mail e sem cadastro.** Concorrentes falham em entregar os convites
  ("only sent 2 of 10 emails out"); aqui não há servidor que possa falhar.
- **Tudo grátis.** Vários cobram para enviar por SMS. Este monetiza só com
  anúncios.
- **O organizador não vê os pares.** Reclamação recorrente: o app manda as
  mensagens pelo celular do organizador e estraga a surpresa para ele.
- **Sorteio matematicamente correto.** Há relato de concorrente que sorteou
  gente duas vezes e deixou gente de fora. Ver a garantia abaixo.

## Identidade visual

A arte-chave está em `playstore/art/` e define a paleta do app
(`ui/theme/Color.kt`): roxo de manto, dourado de brasão e o azul do cristal do
cajado, sobre fundo violeta-tinta.

O registro é **sorteio mágico**, não Natal. Isso distancia o app do LuckyDraw e
do vermelho-e-verde de clipart, e ainda evita que ele pareça descartável fora de
dezembro — amigo secreto de empresa e de confraternização acontece o ano todo.

## A garantia do sorteio

`draw/SecretSantaEngine.kt` produz uma permutação sem pontos fixos que respeita
os pares bloqueados. Disso decorre que **toda pessoa presenteia exatamente uma
vez e é presenteada exatamente uma vez** — ninguém fica de fora, ninguém é
sorteado duas vezes, ninguém tira a si mesmo.

Duas fases:

1. **Ciclo aleatório.** Uma permutação cíclica única nunca tem ponto fixo, então
   já nasce válida quando não há restrições — o caso da maioria dos sorteios. De
   quebra rende uma corrente só, sem pares que se tiram mutuamente.
2. **Busca exata.** Se as restrições derrubarem as tentativas acima, cai para um
   emparelhamento bipartido (Kuhn) com ordens embaralhadas. É polinomial e
   completo: se existe solução, ele acha. Por isso `IMPOSSIBLE_RESTRICTIONS` é
   uma afirmação, não um chute — o app nunca diz "não deu" por ter desistido.

O teste `veredito bate com a forca bruta em todos os casos pequenos` confere o
veredito da engine contra a enumeração de **todas** as permutações, em grupos
sorteados aleatoriamente, e falha se algum dos dois lados não for exercitado.

```bash
./gradlew :app:testDebugUnitTest
```

## Revelação por link

Cada participante recebe um link `https://secretsanta.borzanti.com/r/#<token>`.
O resultado vai **no fragmento**, que o navegador nunca envia ao servidor: a
página em `site/r/index.html` decodifica tudo no aparelho de quem abriu. O
formato está em `share/RevealLink.kt` e é um contrato — o teste
`vetor fixo compartilhado com a pagina web` quebra se ele mudar.

O token é ofuscado (XOR + base64url), **não criptografado**, e o código diz isso
com todas as letras. É suficiente porque cada link contém só o par daquela pessoa.

Para testar a página no celular sem publicar:

```bash
cd site && "D:/Program Files/Android/Android Studio/jbr/bin/jwebserver.exe" -p 8765 -b 127.0.0.1
adb reverse tcp:8765 tcp:8765
```

e abrir `http://localhost:8765/r/#<token>` no Chrome do aparelho. Um token real
sai de `adb exec-out run-as com.borzanti.secretsanta.debug cat files/datastore/secretsanta.preferences_pb`.

## Pendências antes de publicar

- [ ] **AdMob:** criar as unidades deste app (mesmo publisher) e preencher
      `PROD_BANNER`/`PROD_INTERSTITIAL` em `ads/AdIds.kt` e o App ID real no
      `AndroidManifest.xml`. Até lá o release não pede anúncio nenhum — falha
      fechada de propósito.
- [ ] **Site:** publicar `site/` no GitHub Pages com o domínio
      `secretsanta.borzanti.com` (o `CNAME` já está pronto) e criar o registro
      DNS. Sem isso, os links enviados não abrem.
- [ ] Ícone próprio e `playstore/` com screenshots nos 3 idiomas.
- [ ] Keystore de upload própria em `~/keystores` + `keystore.properties`.
- [ ] Na Play Console: política de privacidade em
      `https://secretsanta.borzanti.com/privacy/`.
