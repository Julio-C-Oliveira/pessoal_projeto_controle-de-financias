# Spec 06: Backup e Restauração de Dados Local (JSON)

## 1. Visão Geral
Esta especificação define o mecanismo de exportação e importação de dados para persistência externa em arquivo `.json`. Por ser uma aplicação estritamente offline, o usuário deve ser capaz de salvar o estado completo do banco de dados no armazenamento local do dispositivo (via Storage Access Framework - SAF) e restaurá-lo em caso de migração de aparelho ou reinstalação.

## 2. Estrutura do Payload de Backup

O payload exportado deve conter metadados de controle de versão e os arrays de dados de todas as entidades do banco:

```json
{
  "version": 1,
  "exportedAt": 1772614800000,
  "categories": [],
  "transactions": [],
  "investments": [],
  "investmentContributions": [],
  "monthlyBudgets": []
}
