# Terra Pulse — Aplicativo Android

Aplicativo Android para monitoramento e atualização de firmware da estação meteorológica **Terra Pulse V4.0.4**.

## Funcionalidades

### Dashboard
- Exibe em tempo real os dados da estação (temperatura, umidade, vento, condição de voo, bateria)
- Carrega a página diretamente do ESP32 via WebView
- Reconecta automaticamente a cada 5 segundos enquanto o ESP32 não estiver disponível
- Indicador de conexão (● verde = conectado, ● vermelho = sem resposta)

### Atualizar Firmware (OTA)
- Seleciona o arquivo `.bin` diretamente da pasta Downloads do celular
- Envia o firmware para o ESP32 via HTTP multipart POST
- Barra de progresso em tempo real durante o envio
- O ESP32 reinicia automaticamente após a gravação

## Como usar

### Dashboard
1. Abra o app — a aba **Dashboard** é aberta automaticamente
2. O app tenta conectar ao ESP32 a cada 5s até a página carregar
3. Se o IP do ESP32 mudar, toque em **Editar** na barra superior

### Atualizar Firmware
1. Baixe o arquivo `firmware.bin` para a pasta **Downloads** do celular
2. Ligue o ESP32 — a janela OTA fica aberta por **30 segundos** após conectar ao WiFi (ou 5 minutos segurando o botão BOOT ao ligar)
3. No app, vá na aba **Firmware** (🚀)
4. Toque em **Selecionar firmware (.bin)** e escolha o arquivo
5. Toque em **Enviar para o ESP32** dentro da janela OTA
6. Aguarde a gravação — o ESP32 reinicia sozinho ao concluir

## Configuração

O IP padrão do ESP32 é `192.168.15.2`. Para alterar:
- Toque no botão **Editar** ao lado do IP na barra superior
- Digite o novo IP e confirme

O IP é salvo automaticamente para os próximos usos.

## Instalação do APK

O APK compilado está disponível em:
```
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar no celular:
1. Transfira o arquivo `app-debug.apk` para o celular
2. Abra o arquivo no gerenciador de arquivos
3. Permita a instalação de fontes desconhecidas se solicitado

## Firmware

O firmware do ESP32 está no repositório:
[Terra Pulse V4.0.4 — Firmware](https://github.com/leviguima/stacao_Metereologica_Terra_Pulse_V4_0_4)

## Requisitos

- Android 7.0 (API 24) ou superior
- ESP32 com firmware Terra Pulse V4.0.4 na mesma rede WiFi
