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

# Como vai ser o armazenamento desses dados?
Vou armazenar em um banco de dados, estruturado ou não estruturado?
- Estruturado: Facilita as buscas e diminui os erros. Categorias Fixas com atributos fixos.
- Não estruturado: Possibilidade várias formas diferentes. As categorias podem ser definidas posteriormente, sem atributos fixos.

Acho que inicialmente é melhor fazer com o estruturado.

# Como vai ser a estrutura do banco de dados?