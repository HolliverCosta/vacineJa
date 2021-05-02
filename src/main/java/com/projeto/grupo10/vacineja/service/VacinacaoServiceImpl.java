package com.projeto.grupo10.vacineja.service;

import com.projeto.grupo10.vacineja.model.lote.Lote;
import com.projeto.grupo10.vacineja.model.lote.LoteDTO;
import com.projeto.grupo10.vacineja.model.vacina.Vacina;
import com.projeto.grupo10.vacineja.model.vacina.VacinaDTO;
import com.projeto.grupo10.vacineja.repository.VacinaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class VacinacaoServiceImpl implements VacinacaoService {

    @Autowired
    private VacinaRepository vacinaRepository;

    @Autowired
    private LoteService loteService;

    //TO-DO verificar se/como usar o JWTService no service de Vacina

    /**
     * Cria uma nova Vacina. Caso já exista uma Vacina com o mesmo nome de Fabricante, uma exceção irá ser lançada.
     * @param vacinaDTO eh o DTO da Vacina a ser criada
     * @return a vacina cadastrada
     */
    @Override
    public Vacina criaVacina(VacinaDTO vacinaDTO) {
        Optional<Vacina> optionalVacina = vacinaRepository.findById(vacinaDTO.getNomeFabricante());

        if(optionalVacina.isPresent()){
            throw new IllegalArgumentException("Já existe vacina desse fabricante cadastrada!");
        }

        validaDiasEntreDoses(vacinaDTO.getDiasEntreDoses());
        validaNumDoses(vacinaDTO.getNumDosesNecessarias());

        Vacina vacina = new Vacina(vacinaDTO.getNomeFabricante(), vacinaDTO.getNumDosesNecessarias(), vacinaDTO.getDiasEntreDoses(), vacinaDTO.getQtdDoses());

        vacinaRepository.save(vacina);

        return vacina;
    }

    /**
     * Retorna uma lista das vacinas armazenadas
     * @return lista das vacinas armazenadas em VacinaRepository
     */
    @Override
    public List<Vacina> listarVacinas() {
        return vacinaRepository.findAll();
    }

    /**
     * Retorna um Optional<Vacina> baseada no seu Id (caso nao exista retornara empty)
     * @param nomeFabricante eh o nome do fabricante da Vacina procurada
     * @return optional da Vacina procurada
     */
    @Override
    public Optional<Vacina> getVacinaById(String nomeFabricante) {
        return this.vacinaRepository.findById(nomeFabricante);
    }


    /**
     * Remove qtdDoses de Vacina disponivel em Lotes
     * @param nomeFabricante eh o nome do fabricante da Vacina
     */
    @Override
    public void removeDosesVacina(String nomeFabricante, int qtdDoses){
        Vacina vacina = fetchVacina(nomeFabricante);

        if(!verificaEstoque(nomeFabricante,qtdDoses)){
            //TO-DO Ver se existe outra exceção mais adequada a "não ha doses suficiente de vacina"
            throw new ArrayIndexOutOfBoundsException("Não ha doses suficiente de vacina!");
        }

        for(int i =0; i< qtdDoses; i++) {
            loteService.removeDoseLotes(nomeFabricante);
        }

    }

    /**
     * Vê se existem qtdDoses suficentes de Vacina suficientes guardade em Lotes
     * @param nomeFabricante eh o nome do fabricante da Vacina
     * @param qtdDoses eh o num de de doses buscadas
     * @return booleano que informa se há ou não doses suficientes
     */
    @Override
    public boolean verificaEstoque(String nomeFabricante, int qtdDoses) {
        return loteService.verificaQtdDoseLotes(nomeFabricante, qtdDoses);
    }

    /**
     * Cria um lote de Vacina
     * @param loteDTO DTO do Lote de Vacina
     * @return Lote de Vacina criado
     */
    @Override
    public Lote criaLote(LoteDTO loteDTO){
        Vacina vacina = fetchVacina(loteDTO.getNomeFabricanteVacina());
        return loteService.criaLote(loteDTO, vacina);
    }


    // TO-DO Verificar o comportamento do lançamento da exceção, se
    // existe algum problema no tratamento embaixo de tantas camadas de metodos.

    /**
     * Busca a Vacina em VacinaRepository, se encontrar ele a retorna, se não lança uma exceção
     * @param nomeFabricante eh o nome do fabricante da Vacina
     * @return a vacina procurada
     */
    @Override
    public Vacina fetchVacina(String nomeFabricante) throws NullPointerException{
        Optional<Vacina> optionalVacina = getVacinaById(nomeFabricante);
        if(optionalVacina.isEmpty()){
            throw new NullPointerException("Não há Vacina desse fabricante cadastrada!");
        }
        return optionalVacina.get();
    }

    private void validaNumDoses(int numDosesNecessarias){
        if(numDosesNecessarias > 2 || numDosesNecessarias < 0){
            throw new IllegalArgumentException("Número de doses inválido");
        }
    }

    private void validaDiasEntreDoses(int diasEntreDoses){
        if(diasEntreDoses < 0 || diasEntreDoses > 90){
            throw new IllegalArgumentException("Quantidade de dias entre doses inválido");
        }
    }

}
// Null pointer = nao ha vacina cadastrada
// Illegal argument = ja existe vacina cadastrada
// arrayindexoutofbound = nao ha doses suficientes para ministrar