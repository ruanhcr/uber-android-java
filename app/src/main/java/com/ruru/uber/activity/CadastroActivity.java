package com.ruru.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.ruru.uber.R;
import com.ruru.uber.config.ConfiguracaoFirebase;
import com.ruru.uber.helper.UsuarioFirebase;
import com.ruru.uber.model.Usuario;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private Switch switchTipoUsuario;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        inicializarComponentes();
    }

    private void inicializarComponentes(){
        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);
    }

    public void validarCadastroUsuario(View view){
        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();
        
        if (!textoNome.isEmpty()){
            if (!textoEmail.isEmpty()){
                if (!textoSenha.isEmpty()){
                    Usuario usuario = new Usuario();
                    usuario.setNome(textoNome);
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());
                    cadastrarUsuario(usuario);
                }else{
                    Toast.makeText(CadastroActivity.this, "Preencha a senha!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(CadastroActivity.this, "Preencha o email!", Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(CadastroActivity.this, "Preencha o nome!", Toast.LENGTH_SHORT).show();
        }
    }
    public String verificaTipoUsuario(){
        return switchTipoUsuario.isChecked() ? "M" : "P" ;
    }

    public void cadastrarUsuario(Usuario usuario){
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){
                   try {
                       String idUsuario = task.getResult().getUser().getUid();
                       usuario.setId(idUsuario);
                       usuario.salvar();

                       //atualizar o nome no UserProfile
                       UsuarioFirebase.atualizaNomeUsuario(usuario.getNome());

                       //redireciona o usuário com base no tipo
                       if(verificaTipoUsuario() == "P"){
                           startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                           finish();
                           Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Passageiro!", Toast.LENGTH_SHORT).show();
                       }else{
                           startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                           finish();
                           Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Motorista!", Toast.LENGTH_SHORT).show();
                       }
                   }catch (Exception e) {
                       e.printStackTrace();
                   }
                }else{
                    String excesao = "";
                    try{
                        throw task.getException();
                    }catch(FirebaseAuthWeakPasswordException e){
                        excesao = "Digite uma senha mais forte!";
                    }catch(FirebaseAuthInvalidCredentialsException e){
                        excesao = "Digite um email válido!";
                    }catch (FirebaseAuthUserCollisionException e){
                        excesao = "Essa conta já foi cadastrada!";
                    }catch(Exception e){
                        excesao = "Erro ao cadastrar usuário!" + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(CadastroActivity.this, excesao, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}