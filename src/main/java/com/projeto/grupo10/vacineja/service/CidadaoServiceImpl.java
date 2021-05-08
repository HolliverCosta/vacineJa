package com.projeto.grupo10.vacineja.service;

import com.projeto.grupo10.vacineja.model.usuario.*;
import com.projeto.grupo10.vacineja.repository.CidadaoRepository;
import com.projeto.grupo10.vacineja.repository.FuncionarioGovernoRepository;
import com.projeto.grupo10.vacineja.util.ErroEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.projeto.grupo10.vacineja.util.PadronizaString.padronizaSetsDeString;

import javax.servlet.ServletException;
import java.util.*;

@Service
public class CidadaoServiceImpl implements CidadaoService{

    @Autowired
    private CidadaoRepository cidadaoRepository;

    @Autowired
    private FuncionarioGovernoRepository funcionarioGovernoRepository;

    @Autowired
    private JWTService jwtService;


    @Override
    public Optional<Cidadao> getCidadaoById(String cpf) {
        return this.cidadaoRepository.findById(cpf);
    }

    public void salvarCidadao(Cidadao cidadao){
        this.cidadaoRepository.save(cidadao);
    }

    private FuncionarioGoverno adicionarFuncionarioGoverno(FuncionarioCadastroDTO funcionarioCadastroDTO, String cpfCidadao){
        FuncionarioGoverno funcionarioGoverno = new FuncionarioGoverno(cpfCidadao ,funcionarioCadastroDTO.getCargo(),
                funcionarioCadastroDTO.getLocalTrabalho());

        this.funcionarioGovernoRepository.save(funcionarioGoverno);

        return funcionarioGoverno;
    }

    @Override
    public boolean validaCidadaoSenha (CidadaoLoginDTO cidadaoLogin){
        boolean result = false;

        Optional<Cidadao> cidadao = this.getCidadaoById(cidadaoLogin.getCpfLogin());

        if (cidadao.isPresent()){
            result = cidadao.get().getSenha().equals(cidadaoLogin.getSenhaLogin());
        }

        return result;
    }

    public boolean validaLoginComoFuncionario (CidadaoLoginDTO cidadaoLogin){
        return cidadaoLogin.getTipoLogin().equals("Funcionario") && !this.isFuncionario(cidadaoLogin.getCpfLogin());
    }

    public boolean validaLoginComoAdministrador (CidadaoLoginDTO cidadaoLogin){
        return cidadaoLogin.getTipoLogin().equals("Administrador") && !this.isAdmin(cidadaoLogin.getCpfLogin());
    }

    private boolean isFuncionario(String id){
        boolean result = false;

        Optional<Cidadao> cidadao = this.getCidadaoById(id);

        if (cidadao.isPresent()){
            result = cidadao.get().isFuncionario();
        }

        return result;
    }

    private boolean loginAsFuncionario(String tipoLogin){ return tipoLogin.equals("Funcionário");}

    public void cadastroFuncionario(String headerToken, FuncionarioCadastroDTO cadastroFuncionario) throws ServletException {
        String id = jwtService.getCidadaoDoToken(headerToken);

        Optional<Cidadao> cidadaoOpt = this.getCidadaoById(id);

        if (!cidadaoOpt.isPresent()){
            throw new IllegalArgumentException();
        }

        Cidadao cidadao;
        cidadao = cidadaoOpt.get();
        cidadao.setFuncionarioGoverno(this.adicionarFuncionarioGoverno(cadastroFuncionario, id));

        this.salvarCidadao(cidadao);
    }

    public ArrayList<String> getUsuariosNaoAutorizados() throws ServletException {
        ArrayList<String> funcionariosNaoAutorizados = new ArrayList<String>();
        for (Cidadao cidadao : this.cidadaoRepository.findAll()){
            if (cidadao.aguardandoAutorizacaoFuncionario()){
                funcionariosNaoAutorizados.add(cidadao.getCpf());
            }
        }
        return funcionariosNaoAutorizados;
    }

    public void autorizarCadastroFuncionario(String cpfFuncionario)throws ServletException{
        Optional<Cidadao> cidadaoOpt = this.getCidadaoById(cpfFuncionario);

        if (!cidadaoOpt.isPresent() || !cidadaoOpt.get().aguardandoAutorizacaoFuncionario()){
            throw new IllegalArgumentException();
        }

        Cidadao cidadao = cidadaoOpt.get();
        cidadao.autorizaCadastroFuncionario();

        this.salvarCidadao(cidadao);
    }

    private boolean isAdmin(String id){
        return id.equals("00000000000");
    }
    private boolean loginAsAdmin(String tipoLogin){ return tipoLogin.equals("Administrador");}
    public String teste(String authorizationHeader) throws ServletException {
        String id = jwtService.getCidadaoDoToken(authorizationHeader);
        String tipoLogin = jwtService.getTipoLogin(authorizationHeader);

        if(!isAdmin(id) || !loginAsAdmin(tipoLogin))
            throw new ServletException("Usuario não é admin");

        return "Oie deu certo";
    }


    public void verificaTokenFuncionario(String authHeader) throws ServletException {
        String id = jwtService.getCidadaoDoToken(authHeader);
        String tipoLogin = jwtService.getTipoLogin(authHeader);

        if(!isFuncionario(id))
            throw new ServletException("Usuario não é Funcionário cadastrado!");

    }

    public void cadastraCidadao(CidadaoDTO cidadaoDTO) {
        Optional<Cidadao> cidadaoOpt = this.getCidadaoById(cidadaoDTO.getCpf());
        if(cidadaoOpt.isPresent()){
            throw new IllegalArgumentException("Cidadao cadastrado");
        }
        if(!ErroEmail.validarEmail(cidadaoDTO.getEmail())){
            throw new IllegalArgumentException("Email invalido");
        }
    	Cidadao cidadao = new Cidadao(cidadaoDTO.getNome(), cidadaoDTO.getCpf(), cidadaoDTO.getEndereco(),
    			cidadaoDTO.getCartaoSus(),cidadaoDTO.getEmail() ,cidadaoDTO.getData_nascimento(),cidadaoDTO.getTelefone(),
    			padronizaSetsDeString(cidadaoDTO.getProfissoes()),padronizaSetsDeString(cidadaoDTO.getComorbidades()),
                cidadaoDTO.getSenha());
    	this.salvarCidadao(cidadao);

    }

    @Override
    public Cidadao updateCidadao(String headerToken, CidadaoUpdateDTO cidadaoUpdateDTO, Cidadao cidadao) throws ServletException {

        String id = jwtService.getCidadaoDoToken(headerToken);

        Optional<Cidadao> cidadaoOpt = this.getCidadaoById(id);

        if (!cidadaoOpt.isPresent()){
            throw new IllegalArgumentException();
        }

        cidadao.setCartaoSus(Objects.nonNull(cidadaoUpdateDTO.getCartaoSus()) ? cidadaoUpdateDTO.getCartaoSus() : cidadao.getCartaoSus());
        cidadao.setComorbidades(Objects.nonNull(cidadaoUpdateDTO.getComorbidades()) ? (padronizaSetsDeString(cidadaoUpdateDTO.getComorbidades())) : cidadao.getComorbidades());
        cidadao.setData_nascimento(Objects.nonNull(cidadaoUpdateDTO.getData_nascimento()) ? cidadaoUpdateDTO.getData_nascimento() : cidadao.getData_nascimento());
        cidadao.setEmail(Objects.nonNull(cidadaoUpdateDTO.getEmail()) ? cidadaoUpdateDTO.getEmail() : cidadao.getEmail());
        cidadao.setEndereco(Objects.nonNull(cidadaoUpdateDTO.getEndereco()) ? cidadaoUpdateDTO.getEndereco() : cidadao.getEndereco());
        cidadao.setSenha(Objects.nonNull(cidadaoUpdateDTO.getSenha()) ? cidadaoUpdateDTO.getSenha() : cidadao.getSenha());
        cidadao.setNome(Objects.nonNull(cidadaoUpdateDTO.getNome()) ? cidadaoUpdateDTO.getNome() : cidadao.getNome());
        cidadao.setTelefone(Objects.nonNull(cidadaoUpdateDTO.getTelefone()) ? cidadaoUpdateDTO.getTelefone() : cidadao.getTelefone());
        cidadao.setProfissoes(Objects.nonNull(cidadaoUpdateDTO.getProfissoes()) ? padronizaSetsDeString(cidadaoUpdateDTO.getProfissoes()) : cidadao.getProfissoes());
        return cidadao;
    }


}
