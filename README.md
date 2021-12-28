# Uber Clone
Aplicativo desenvolvido em curso que utiliza algumas abordagens do App real Uber.
<br>Conta com cadastro do usuário, login e autenticação. Sendo possível cadastrar-se como passageiro ou como motorista.
<br>O projeto consome a API [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/overview?hl=pt_BR) no console do [Google Cloud Platform](https://console.cloud.google.com/apis/dashboard)
para se utilizar os mapas. <br>É necessário ativação, criar uma credencial para gerar a chave e inseri-la na String "google_maps_key" dentro do arquivo xml: google_maps_api.xml.
<br>É necessário configurar um projeto no [Firebase](https://accounts.google.com/signin/v2/identifier?passive=1209600&osid=1&continue=https%3A%2F%2Fconsole.firebase.google.com%2F%3Fhl%3Dpt-br&followup=https%3A%2F%2Fconsole.firebase.google.com%2F%3Fhl%3Dpt-br&hl=pt-br&flowName=GlifWebSignIn&flowEntry=ServiceLogin) e substituir o arquivo google-services.json gerado para que o app utilize o banco de dados.</br>

Exemplo prático de uma corrida aceita: 
<br>Passageiro (emulador esquerdo) e motorista (a direita)
<br>![exemplo](https://i.imgur.com/doMJZs7.png)
