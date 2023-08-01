# X-Road: Autologin User Guide

Version: 1.4
Doc. ID: UG-AUTOLOGIN


| Date       | Version | Description                                                                                       |
|------------|---------|---------------------------------------------------------------------------------------------------|
| 23.08.2017 | 1.0     | Initial version                                                                                   |
| 06.03.2018 | 1.1     | Added chapter and section structure, terms and refs sections and term doc reference and link, toc |
| 15.11.2018 | 1.2     | Ubuntu 18.04 updates                                                                              |
| 11.09.2019 | 1.3     | Remove Ubuntu 14.04 support                                                                       |
| 26.09.2022 | 1.4     | Remove Ubuntu 18.04 support                                                                       |
| 01.08.2023 | 1.5     | Tradução e atualizações no texto                                                                  |

## Table of Contents

<!-- toc -->

- [1 Introduction](#1-introduction)
    + [1.1 Terms and abbreviations](#11-terms-and-abbreviations)
    + [1.2 References](#12-references)
- [2 Overview](#2-overview)
    + [2.1 Usage](#21-usage)
    + [2.2 Implementation details](#22-implementation-details)
    
<!-- tocstop -->

## 1 Introdução

Este documento descreve o utilitário de Autologin responsável por inserir automaticamente o código PIN após o início do `xroad-signer`.

### 1.1 Termos e abreviações

Consulte a documentação de termos abreviações X-Via \[[TA-TERMS](#Ref_TERMS)\].

### 1.2 Referências

1. <a id="Ref_TERMS" class="anchor"></a>\[TA-TERMS\] X-Road Terms and Abbreviations. Document ID: [TA-TERMS](../../terms_x-road_docs.md).

## 2 Visão geral
### 2.1 Passo a passo

1. Instale o pacote
  * Ubuntu: apt install xroad-autologin
  * RedHat: yum install xroad-autologin

2. Se armazenar o código PIN no servidor em texto simples for aceitável, crie um arquivo `/etc/xroad/autologin` que contenha o código PIN. 
  * O arquivo deve ser legível pelo usuário `xroad`
  * Se `/etc/xroad/autologin` não existir e você não tiver implementado `custom-fetch-pin.sh`, o serviço não será iniciado.
3. Se você não deseja armazenar o código PIN em texto sem formatação, implemente o script bash 
`/usr/share/xroad/autologin/custom-fetch-pin.sh`
  * O script precisa enviar o código PIN para stdout
  * O script deve ser legível e executável pelo usuário `xroad`
  * O script deve sair com o código de saída
    * 0 se foi capaz de buscar o código PIN com sucesso
    * 127 se não foi possível buscar o código PIN, mas este não é um erro real que deve causar falha no serviço (a implementação padrão usa isso se `/etc/xroad/autologin` não existir)
    * outros códigos de saída em situações de erro que devem causar falha no serviço
  ```bash
  #!/bin/bash
  PIN_CODE=$(curl https://some-address)
  echo "${PIN_CODE}"
  exit 0
  ```

### 2.2 Detalhes da implementação

* Crie um novo `xroad-autologin`
* O serviço é iniciado após `xroad-signer`
* No RHEL/Ubuntu 20.04, o serviço chama o script wrapper `/usr/share/xroad/autologin/xroad-autologin-retry.sh` que, por sua vez, chama o `autologin.expect`
  * O script wrapper lida com novas tentativas em situações de erro.
* O serviço tenta inserir o código PIN usando `signer-console`
  * Se o PIN estiver correto ou incorreto, é retirado
  * Se ocorre um erro (por exemplo, o `xroad-signer` ainda não foi totalmente iniciado), ele continua tentando fazer o login indefinidamente
