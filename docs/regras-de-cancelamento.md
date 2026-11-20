# FAIRGO — Regras de Cancelamento

## 1. Objetivo

Estabelecer regras transparentes de cancelamento, protegendo passageiros e motoristas contra cobranças injustas e prejuízos decorrentes de deslocamentos.

As regras devem considerar a etapa da corrida, a distância percorrida pelo motorista e o tempo de espera.

## 2. Cancelamento durante o deslocamento até o passageiro

Quando o passageiro cancelar após o motorista aceitar a solicitação:

- Registrar a distância inicialmente prevista até o embarque.
- Registrar a distância efetivamente percorrida pelo motorista.
- Verificar se o motorista percorreu mais de 50% da distância prevista.
- A compensação por deslocamento somente poderá ser aplicada quando esse limite for ultrapassado.
- A cobrança deverá considerar a distância efetivamente percorrida, não a distância total prevista.

### 2.1. Valores por quilômetro em avaliação

| Distância prevista até o embarque | Compensação proposta por km percorrido |
|---|---|
| Até 3 km | R$ 1,70/km |
| Acima de 3 até 5 km | R$ 1,50/km |
| Acima de 5 até 8 km | R$ 1,30/km |
| Acima de 8 até 12 km | R$ 1,15/km |
| Acima de 12 km | R$ 1,10/km |

**Status:** a tabela completa ainda precisa de aprovação. Os valores refletem a proposta de remuneração decrescente conforme a distância prevista.

Exemplos discutidos:
- Deslocamento previsto de 2 km, com mais de 1 km percorrido: referência de R$ 1,70/km.
- Deslocamento previsto de 10 km, com 6 km percorridos: referência de R$ 1,15/km.
- Deslocamentos maiores: referência de R$ 1,10/km.

## 3. Cancelamento após chegada ao embarque

A distância prevista até o embarque determina o tipo de compensação.

### 3.1. Deslocamentos curtos — até 5 km

- Taxa fixa de cancelamento: R$ 4,00.
- Acrescentar a cobrança de espera excedente, quando aplicável.
- Não acumular a taxa fixa com outra compensação por quilometragem.

### 3.2. Deslocamentos longos — acima de 5 km

- Aplicar a compensação por quilômetro efetivamente percorrido até o embarque.
- Somar separadamente o valor da espera excedente.
- Não acrescentar a taxa fixa de R$ 4,00.

Os valores por quilômetro dependem da aprovação definitiva da tabela de compensação.

## 4. Cobrança por tempo de espera no embarque

Regras definidas:

- De 0 a 1 minuto e 29 segundos: nenhuma cobrança.
- Ao completar 1 minuto e 30 segundos: R$ 0,20.
- Ao completar 2 minutos e 30 segundos: R$ 0,40 acumulados.
- Ao completar 3 minutos e 30 segundos: R$ 0,60 acumulados.
- A cobrança continua aumentando R$ 0,20 a cada minuto adicional completo após o primeiro marco.

A cobrança é feita por intervalos, sem proporcionalidade por segundos entre os marcos.

O sistema deverá registrar o horário de chegada e o início efetivo da contagem.

## 5. Pedágios

- Quando houver pedágio elegível relacionado ao deslocamento ou cancelamento, a FAIRGO deverá realizar o ressarcimento automático conforme a política aplicável.
- O sistema deverá registrar o valor e a justificativa do ressarcimento.
- Ainda é necessário definir os critérios de comprovação, elegibilidade e prevenção de duplicidades.

## 6. Proteção contra cobranças indevidas

O sistema deverá:

- Registrar os horários de aceitação, chegada e cancelamento.
- Utilizar registros confiáveis de localização e deslocamento.
- Impedir a cobrança duplicada de taxa fixa e compensação por quilometragem.
- Separar a compensação por deslocamento da cobrança de espera.
- Disponibilizar o detalhamento da cobrança para consulta.
- Permitir contestação de valores.

## 7. Situações ainda pendentes

Precisam de definição antes da implementação definitiva:

- Cancelamento realizado pelo motorista.
- Cancelamento antes de um motorista aceitar a solicitação.
- Passageiro não localizado no ponto de embarque.
- Prazo máximo de espera antes do encerramento por ausência.
- Falhas de GPS, conexão ou funcionamento do aplicativo.
- Situações de emergência e segurança.
- Cancelamentos motivados por divergência de identidade.
- Critérios de isenção, reembolso e contestação.
- Destinação da taxa de cancelamento e eventual comissão da plataforma.
- Aprovação final da tabela de compensação por quilômetro.

## 8. Status das definições

**Regras definidas:**
- Limite de 50% do deslocamento previsto para compensação por cancelamento durante o trajeto.
- Distinção entre deslocamentos curtos e longos no embarque: 5 km.
- Taxa fixa de R$ 4,00 para cancelamento no embarque em deslocamentos curtos.
- Compensação por quilometragem no embarque para deslocamentos acima de 5 km.
- Cobrança separada da espera excedente.
- Marcos de cobrança de espera de R$ 0,20.
- Previsão de ressarcimento automático de pedágios elegíveis.

**Em avaliação:**
- Tabela definitiva de valores por quilômetro.
- Regras operacionais para situações excepcionais.

Não implementar decisões pendentes como se fossem regras aprovadas.