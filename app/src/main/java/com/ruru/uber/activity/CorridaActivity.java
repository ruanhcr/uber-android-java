package com.ruru.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ruru.uber.R;
import com.ruru.uber.config.ConfiguracaoFirebase;
import com.ruru.uber.helper.Local;
import com.ruru.uber.helper.UsuarioFirebase;
import com.ruru.uber.model.Destino;
import com.ruru.uber.model.Requisicao;
import com.ruru.uber.model.Usuario;

import java.text.DecimalFormat;

public class CorridaActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Button buttonAceitarCorrida;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private Boolean requisicaoAtiva;
    private FloatingActionButton fabRota;
    private Destino destino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_corrida);

        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        buttonAceitarCorrida = findViewById(R.id.buttonAceitarCorrida);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar Corrida");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //configura o usuario
        if (getIntent().getExtras().containsKey("idRequisicao") && getIntent().getExtras().containsKey("motorista")){
            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();

            //adicionar evento de click no FAB
            fabRota = findViewById(R.id.fabRota);
            fabRota.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String status = statusRequisicao;
                    if(status != null && !status.isEmpty()){
                        String lat = "";
                        String lon = "";
                        switch (status){
                            case Requisicao.STATUS_A_CAMINHO:
                                lat = String.valueOf(localPassageiro.latitude);
                                lon = String.valueOf(localPassageiro.longitude);
                                break;
                            case Requisicao.STATUS_VIAGEM:
                                lat = destino.getLatitude();
                                lon = destino.getLongitude();
                                break;
                        }
                        //abrir rota
                        String latLong = lat + "," + lon;
                        Uri gmmIntentUri = Uri.parse("google.navigation:q"+latLong+"&mode=d");
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        startActivity(mapIntent);
                    }
                }
            });
        }
    }

    private void verificaStatusRequisicao(){
        DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //recuperar requisição
                requisicao = snapshot.getValue(Requisicao.class);
                if(requisicao != null){
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude()));
                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void alteraInterfaceStatusRequisicao(String status){
        switch (status){
            case Requisicao.STATUS_AGUARDANDO:
                requisicaoAguardando();
                break;
            case Requisicao.STATUS_A_CAMINHO:
                requisicaoACaminho();
                break;
            case Requisicao.STATUS_VIAGEM:
                requisicaoViagem();
                break;
            case Requisicao.STATUS_FINALIZADA:
                requisicaoFinalizada();
                break;
            case Requisicao.STATUS_CANCELADA:
                requisicaoCancelada();
                break;
        }
    }

    private void requisicaoAguardando(){
        buttonAceitarCorrida.setText("Aceitar Corrida");

        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        centralizarMarcador(localMotorista);

    }

    private void requisicaoACaminho(){
        buttonAceitarCorrida.setText("A caminho do passageiro");

        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //exibe marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //centralizar dois marcadores
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

        //inicia monitoramento do motorista / passageiro
        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);

        //exibe o fab
        fabRota.setVisibility(View.VISIBLE);
    }

    private void requisicaoViagem(){
        //altera interface
        fabRota.setVisibility(View.VISIBLE);
        buttonAceitarCorrida.setText("A caminho do destino");

        //exibe marcador do motorista
        adicionarMarcadorMotorista(localMotorista, motorista.getNome());

        //exibe marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                        Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");

        //centraliza marcadores motorista / destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

        //inicia monitoramento do motorista / passageiro
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);
    }

    private void requisicaoFinalizada(){
        fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;
        if(marcadorMotorista != null)
            marcadorMotorista.remove();
        if(marcadorDestino != null)
            marcadorDestino.remove();
        //exibe marcador de destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLongitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionarMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 8;
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);
        buttonAceitarCorrida.setText("Corrida finalizada - R$ " + resultado);
    }

    private void requisicaoCancelada(){
        Toast.makeText(this, "Requisição foi cancelada pelo passageiro", Toast.LENGTH_LONG).show();
        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));
    }

    private void iniciarMonitoramento(final Usuario usuarioOrigem, LatLng localDestino, final String status){
        //inicializar geofire
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //adiciona circulo na area do passageiro
        Circle circulo = mMap.addCircle(
                new CircleOptions()
                .center(localDestino)
                .radius(25) //em metros
                .fillColor(Color.argb(90, 255, 153, 0))
                .strokeColor(Color.BLACK)
        );
        GeoQuery geoQuery = geoFire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.25 //em km
        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(key.equals(usuarioOrigem.getId())){
                    //altera status da requisicao
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();

                    //remove listener
                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());
        LatLngBounds bounds = builder.build();
        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno)
        );
    }

    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 15)
        );
    }

    private void adicionarMarcadorMotorista(LatLng localizacao, String titulo){
        if(marcadorMotorista != null)
            marcadorMotorista.remove();
        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );

    }

    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo){
        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();
        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );
    }

    private void adicionarMarcadorDestino(LatLng localizacao, String titulo){
        if(marcadorPassageiro != null)
            marcadorPassageiro.remove();
        if(marcadorDestino != null)
            marcadorDestino.remove();
        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        recuperarLocalizacaoUsuario();
    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //recuperar latitude e longitude
                Double latitude = location.getLatitude();
                Double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);

                //atualizar no geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //atualizar localização do motorista no firebase
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                alteraInterfaceStatusRequisicao(statusRequisicao);

            }
        };
        //solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }
    }

    public void aceitarCorrida(View view){
        //configura a requisicao
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);
        requisicao.atualizar();

    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva){
            Toast.makeText(CorridaActivity.this, "Necessário encerrar a requisição atual!", Toast.LENGTH_SHORT).show();
        }else{
            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(i);
        }
        //verificar o status da requisição para encerrar
        if(statusRequisicao != null && !statusRequisicao.isEmpty()){
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizar();
        }
        return false;
    }
}