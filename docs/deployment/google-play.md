# Gerar versão assinada e enviar ao Google Play

Fluxo aprovado: GitHub Actions executa verificações e gera APK/AAB assinados;
o proprietário envia o AAB manualmente pelo Play Console. Não configurar Google
Cloud, conta de serviço, Fastlane ou PLAY_SERVICE_ACCOUNT_JSON.

## 1. Verificar e bloquear custos do GitHub

Na conta proprietária do repositório, abra Settings → Billing and licensing →
Budgets and alerts. Confira a franquia disponível e configure gasto adicional
zero para Actions com Stop usage when budget limit is reached ativado.
Verifique também os controles de armazenamento e cache. Alertas sozinhos não
bloqueiam cobrança. Se o bloqueio não estiver disponível, pause antes de executar
workflows para avaliar geração local. Não contratar plano nem aumentar orçamento.

A pipeline usa runner Linux padrão e guarda artifacts por um dia. Isso reduz
consumo, mas não garante custo zero: franquias são compartilhadas com outros
repositórios e a pipeline não consulta o faturamento da conta.

## 2. Conferir a assinatura existente

Localize o keystore (.jks ou .keystore), a senha do arquivo, o alias e a senha
da chave. No Linux, substitua o caminho abaixo e digite a senha quando solicitado:

```bash
/opt/android-studio/jbr/bin/keytool -list -v \
  -keystore "/caminho/para/seu-arquivo.jks"
```

Confirme no Play Console que o pacote é com.m4.red_android. Se o app usa Play
App Signing, compare o SHA-256 do keystore com o certificado da chave de upload,
na página de assinatura do app. Use a chave já registrada; se não corresponder,
resolva essa diferença antes de gerar uma atualização.

## 3. Cadastrar os quatro secrets no repositório

No Linux, gere a representação Base64 do keystore:

```bash
base64 -w 0 "/caminho/para/seu-arquivo.jks"
```

No GitHub, repositório red-android → Settings → Secrets and variables → Actions
→ New repository secret, cadastre:

| Nome | Valor |
| --- | --- |
| ANDROID_KEYSTORE_B64 | Saída completa do comando Base64 |
| ANDROID_KEYSTORE_PASSWORD | Senha do arquivo keystore |
| ANDROID_KEY_ALIAS | Alias da chave |
| ANDROID_KEY_PASSWORD | Senha da chave, que pode ser igual à do keystore |

Não enviar esses valores por chat nem commitá-los. Use secrets do repositório,
não variables. Se já foram cadastrados no ambiente production, recadastre nesse
local: o workflow atual não utiliza environments. Restrinja acesso de escrita ao
repositório e revise workflows: secrets do repositório não oferecem aprovação
por ambiente.

## 4. Anotar a versão e preparar testadores

No Play Console, consulte o maior versionCode enviado em todas as faixas e
escolha um inteiro maior para a próxima versão. Anote também o versionName
pretendido, por exemplo 1.1.0. Configure a lista de testadores internos.
Informe ao colaborador apenas os números de versão e as etapas concluídas.

## 5. Gerar após revisão, commit, PR e CI

A pipeline precisa estar integrada em main, com CI passando e custos bloqueados.
No GitHub → Actions → Android release artifact → Run workflow, escolha main e
preencha:

| Campo | Valor |
| --- | --- |
| version_name | Versão visível, por exemplo 1.1.0 |
| version_code | Inteiro maior que todos os já enviados ao Play |
| target_sha | SHA completo do commit atual de main |
| confirmation | RELEASE-ANDROID: seguido do mesmo SHA completo |

O SHA pode ser copiado na página do commit atual de main no GitHub. Se main
avançar antes da execução, a confirmação deve corresponder ao novo commit.
O workflow verifica o SHA, executa testes/lint/contrato, gera APK e AAB assinados
e verifica a assinatura do APK. Execução real ainda depende dos secrets.

## 6. Baixar e enviar o AAB

Na execução concluída do GitHub, abra Artifacts e baixe o pacote
red-android-release-<versão>-<SHA> em até um dia. Extraia o ZIP e localize o .aab.
Guarde os arquivos da release que precisar manter fora do GitHub.

No Play Console, abra o RED → Testar e lançar → Teste → Teste interno.
Crie uma nova versão, envie o AAB, preencha as notas, salve e revise. Resolva
pendências apontadas pelo Console antes de iniciar a disponibilização aos
testadores. Os rótulos podem variar com o idioma e o estado de cadastro do app.

Instale pelo link de teste do Play e valide login, recuperação de senha,
troca obrigatória de senha e comunicação com a API. As verificações operacionais
do backend ECO-T007 continuam pendentes. Após validar o aplicativo e o backend,
a promoção para produção será feita manualmente pelo Play Console.

## Referências

- https://docs.github.com/en/billing/how-tos/set-up-budgets
- https://support.google.com/googleplay/android-developer/answer/9842756?hl=pt-BR
- https://support.google.com/googleplay/android-developer/answer/9859348?hl=pt-BR
