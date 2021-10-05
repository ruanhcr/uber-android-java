package com.ruru.uber.helper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.ruru.uber.activity.PassageiroActivity;
import com.ruru.uber.activity.RequisicoesActivity;
import com.ruru.uber.config.ConfiguracaoFirebase;
import com.ruru.uber.model.Usuario;

import java.util.UUID;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static Usuario getDadosUsuarioLogado(){
        FirebaseUser firebaseUser = getUsuarioAtual();
        Usuario usuario = new Usuario();
        usuario.setId(firebaseUser.getUid());
        usuario.setEmail(firebaseUser.getEmail());
        usuario.setNome(firebaseUser.getDisplayName());
        return usuario;
    }

    public static Boolean atualizaNomeUsuario(String nome){
        try {
            FirebaseUser user = getUsuarioAtual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()){
                        Log.d("Perfil", "Erro ao atualizar nome de perfil");
                    }
                }
            });
            return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public static void redirecionaUsuarioLogado(Activity activity){

        FirebaseUser user = getUsuarioAtual();
        if (user != null){
            DatabaseReference usuariosRef = ConfiguracaoFirebase.getFirebaseDatabase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());
            usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    String tipoUsuario = usuario.getTipo();
                    if(tipoUsuario.equals("M")){
                        activity.startActivity(new Intent(activity, RequisicoesActivity.class));
                    }else{
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    public static void atualizarDadosLocalizacao(Double lat, Double lon){
        //define nó de local de usuario
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geoFire = new GeoFire(localUsuario);

        //recupera dados usuario logado
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        geoFire.setLocation(
                usuarioLogado.getId(),
                new GeoLocation(lat, lon),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if(error != null){
                            Log.d("Erro", "Erro ao salvar local!");
                        }
                    }
                });
    }

    public static String getIdentificadorUsuario(){
        return getUsuarioAtual().getUid();
    }
}
