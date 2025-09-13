Repositório criado para armazenar o projeto de programa de controle de financias

# O que eu quero que o aplicativo faça?
Registre mensalmente os seguintes dados, que serão inseridos manualmente ou automaticamente.
- Quero que registre os seguintes pontos:
    - Receita: Deve registrar o tipo de renda (passiva ou ativa), de onde vem a receita (gercom, sites e etc...), quanto recebe e qual a frequencia.
    - Dívidas: Deve registrar o tipo (cartão, empréstimo e etc... especificando qual cartão é), a origem da dívida (comida, ração e etc...), em quantas parcelas foram e qual o dia de vencimento da parcela.
    - Investimentos: Deve registrar o tipo (renda fixa ou variavél), qual investimento é (tesouro direto, lci, lca e etc...), qual vai ser o tempo de investimento (longo prazo, médio ou curto), registrar aportes e quanto está guardado.
    - Despesas Essenciais: Deve registrar o tipo de despesa (alimentação, transporte, cuidados pessoais e etc...), qual despesa foi (onibûs, recarga do celular, ração e etc...), registrar se ela é única, ou se ela se repete e qual é a frequencia dela e o valor. 
    - Despesas Não Essenciais: Deve registrar o tipo de despesa (alimentação, transporte, cuidados pessoais e etc...), qual despesa foi (doce de leite, cortar o cabelo e etc...), registrar se ela é única, ou se ela se repete e qual é a frequencia dela e o valor.
- Com esses dados eu quero que seja possível:
    - Exibir o quanto foi gasto em cada categoria.
    - Qual foi o gasto anual com tudo.
    - Qual é o gasto mensal com cada categoria.

# Stack de Desenvolvimento
- Linguagem: Kotlin.
- IDE: Android Studio, ou Nvim.
- Banco de Dados:
    - Realm: Banco de dados não estruturado para Android.
    - DataStore: Para armazenar configurações simples.
- Backup e Sincronização: Firebase (Auth + Firestore).
- UI: JetpackCompose.
