# Deploy e instalação — 2026-09-09

## Autorização e limites

Usuário autorizou deploy do restante em produção e instalação Android por USB.
Publicação no Google Play aguardará a ativação da nova chave de upload, prevista
pelo usuário para dois dias. Não contratar serviços pagos nem elevar orçamento.

## Concluído localmente

- Backend: 55 suítes / 434 testes passaram nesta execução; SDD, OpenAPI e lint passaram.
- Web: 17 arquivos / 59 testes passaram; SDD, contrato e build passaram.
- APK debug compilado com JDK do Android Studio, sem download de dependências.
- APK instalado com adb install -r no moto g35 5G conectado; resultado Success.
- MainActivity abriu com Status ok e o processo permaneceu ativo.
- API embutida: https://7700ezljb5.execute-api.us-east-1.amazonaws.com/.
- Esse APK é para teste via USB, assinado com a chave debug existente. A nova
  chave de upload do Play não foi usada. Não foram apagados dados do aplicativo.
- Health da API atual respondeu 200/status ok e o web existente respondeu 200.
  Isso é baseline anterior ao deploy, não prova de implantação desta release.
- Pacote backend, build web e manifesto preparados em
  red-infra/artifacts/release-2026-09-09 (diretório ignorado pelo Git).
- O entrypoint Lambda e o bcrypt do pacote carregam localmente.

## Destino confirmado

A variável production do GitHub red-web confirma a API 7700ezljb5. Os arquivos
IaC modelam a API red-backend e o web drsnmw4ga88b7.cloudfront.net sob live/dev.
O diretório live/prod modela outro bucket/site; não aplicar esse ambiente
automaticamente para atualizar a aplicação atual.

## Implantação concluída

- AWS SSO renovado; conta confirmada: 097604633943, região us-east-1.
- Migration 0005 aplicada no reddb e índices de fila/TTL verificados.
- Terraform API: plano revisado e aplicado com 3 adicionados, 1 alterado, 0 removidos.
- Lambda red-backend atualizada e versão imutável 2 publicada.
- CodeSha256 implantado: 1mIrX8ujhB3m9lRhGci0aRtnP2RRYuK4dVRtcfNBqQU=.
- Scheduler red-dev-password-recovery habilitado a cada minuto; conclusão real
  do worker observada no CloudWatch às 21:13:05 UTC. Fila sem pendências.
- Autenticação SMTP de produção verificada sem enviar e-mail.
- Health 200, rota protegida sem credenciais 401, login sem campos 400,
  preflight CORS 204.
- Web publicado no bucket red-web-dev: index.html e novos assets CSS/JS.
  Assets anteriores preservados; nenhum objeto removido.
- CloudFront E2XHRVYUHIIIIZ: invalidação I79X3V904RQ52SLEF7WYGPVA6 concluída.
- Site: https://drsnmw4ga88b7.cloudfront.net.

## Custo e rastreabilidade

O usuário autorizou o pequeno custo do S3 existente após ser informado da
possibilidade de cobrança. Foram enviados três objetos, aproximadamente 1,36 MB;
a invalidação acrescentou um caminho a dois já usados no mês. O uso Lambda
consultado estava muito abaixo da franquia e o Scheduler possui franquia para
esse volume (~43.200 invocações/mês). Nenhum plano pago foi contratado e nenhum
orçamento foi aumentado. Valor final depende da contabilização AWS; não afirmar
custo zero. O budget existente mostrava US$ 0,002 realizados e US$ 0,013 previstos
antes desta publicação.

Deploy realizado diretamente via AWS com snapshots locais validados. Não foram
criados commits, PRs nem pushes; alterações pré-existentes foram preservadas.
Manifestos, versões S3 e rollback do backend/web estão no diretório ignorado
red-infra/artifacts/release-2026-09-09. O ZIP de rollback contém o código realmente
ativo antes do deploy, cujo hash difere da antiga versão imutável 1.

## Validação ainda pendente

A entrega de e-mail e a jornada autenticada completa em produção não foram
executadas: SMTP autenticado e worker ativo não comprovam entrega ao destinatário.
Validação manual de acessibilidade e capacidade sob carga também permanece.
ECO-T007 não deve ser considerado totalmente concluído por este deploy.
Aguardar ativação da nova chave de upload para testar assinatura da release
pelo GitHub e envio manual ao Play. O app instalado por USB usa assinatura debug.
