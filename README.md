# CREDJAM — MVP de atualização de saldo

Implementação inicial baseada na documentação técnica fornecida. O projeto é uma base demonstrativa e **não declara paridade numérica com o COBOL**.

## Requisitos
- **Java 17 ou superior** instalado e no PATH (download: https://adoptium.net/)
- **Conexão com a internet** na primeira execução (para baixar as dependências)
- Porta 8080 disponível
- Windows (os scripts de execução são `.cmd`)

> Não é necessário ter o Maven instalado: o projeto inclui o **Maven Wrapper** (`mvnw`), que é usado automaticamente pelo script.

## Executar

Baixe o projeto (ZIP ou clone) e, na pasta raiz, dê um duplo-clique em:

```
run.cmd
```

Ou execute pelo terminal:

```bat
run.cmd
```

Esse comando:
- usa o Maven Wrapper (`mvnw`) para compilar o projeto na primeira vez (pode demorar e requer internet);
- localiza (ou gera) o JAR em `target\`;
- inicia a aplicação no console;
- abre o navegador automaticamente em `http://localhost:8080`.

A partir da segunda execução o JAR já existe e o início é imediato.

Para apenas validar os pré-requisitos sem iniciar a aplicação:
```bat
scripts\run-cli.cmd --check
```

Acesse `http://localhost:8080`.

O H2 usa arquivo local em `./data/credjam`. O console H2 fica desabilitado por padrão. Para habilitá-lo localmente, use o perfil `demo` e confira a configuração antes de expor a aplicação.

## Dados de referência
O Flyway cria as tabelas e a migration `V2__carga_referencia_demonstrativa.sql` insere dados fictícios em `FGCRB550`, `FGCRB555` e `FGCRB560`. Isso permite executar o fluxo local sem carga manual e exercitar seleção de coeficientes, vigência de taxa e ramos históricos. Todos os registros usam `FONTE_DADO='DEMONSTRACAO'` e versão `MOCK-COBOL-FLOW-V1`.

**Atenção:** coeficientes, vigências e calendário são dados de teste, não tabelas oficiais do FGTS. Eles permitem validar o encadeamento técnico do MVP, mas não comprovam paridade numérica com o COBOL. A substituição por dados oficiais e a comparação de resultados com casos de referência continuam necessárias antes de uso real.

## Endpoint
Canônico: `POST /api/v1/atualizacoes-conta`

Compatibilidade legada mantida: `POST /api/atualizacoes-conta`

Exemplo:
```json
{
  "origem": "DEMONSTRACAO",
  "dataBase": "2024-01-10",
  "dataLimite": "2024-04-10",
  "saldoInicial": "20000.00",
  "totalDepositosInicial": "18000.00",
  "jamInformadoInicial": "2000.00",
  "saldoComplementarInicial": "0.00",
  "dataOpcao": null,
  "tipoConta": "DEMONSTRACAO",
  "dadosHistoricosCompletos": false
}
```

## Limitações explícitas
- Os schemas das tabelas históricas são modelos lógicos propostos, não layouts físicos confirmados do mainframe.
- O fluxo implementado cobre o ciclo cronológico simples, taxa vigente e multiplicação por coeficiente.
- Os tratamentos históricos `SBPC6973`, `JAM-6973`, conversão monetária e complemento de 01/04/1976 ainda precisam ser codificados e validados diretamente contra os ramos correspondentes do COBOL; não são simulados silenciosamente.
- A formação/elegibilidade do SBPC, os códigos de lançamento e os parâmetros completos de conta precisam dos fontes normativos/COBOL integrais para implementação fiel.
- A política HALF_UP é apenas demonstrativa; confirmar arredondamento do legado.
- A seleção de taxa de 3% para data de opção posterior a 22/09/1971 foi isolada, mas as condições completas devem ser verificadas no fonte COBOL.
- O fechamento JAM usa `saldo - totalDepositosInicial` quando o total é informado; sem ele, retorna soma demonstrativa dos créditos e avisa sobre a limitação.
- Datas de corte seguem a condição estrita `dataCoeficiente < dataLimite`.
- Os testes atuais cobrem apenas a operação matemática isolada. Execute `mvn test` no ambiente local para validar compilação e execução integral.
