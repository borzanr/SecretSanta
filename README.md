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

## Pendências antes de publicar

- [ ] Criar as unidades de anúncio no AdMob e trocar o App ID de teste no
      `AndroidManifest.xml` (hoje aponta para o de teste do Google de propósito:
      um App ID de produção errado derruba o app no start).
- [ ] Copiar `ads/`, `ui/ReviewPrompt.kt` e `data/AppDataStore.kt` do LuckyDraw.
- [ ] Ícone próprio e `playstore/` com screenshots nos 3 idiomas.
- [ ] `site/` com a página de revelação e a política de privacidade, em
      `secretsanta.borzanti.com`.
- [ ] Keystore de upload própria em `~/keystores` + `keystore.properties`.

O roadmap completo, com ASO e cronograma, está no plano da sessão.
