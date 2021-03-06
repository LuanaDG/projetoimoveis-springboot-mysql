package com.luana.projetoimoveis.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import com.luana.projetoimoveis.entities.Imovel;
import com.luana.projetoimoveis.entities.Preco;
import com.luana.projetoimoveis.entities.Venda;
import com.luana.projetoimoveis.exception.BusinessException;
import com.luana.projetoimoveis.exception.DataBaseException;
import com.luana.projetoimoveis.exception.ResourceNotFoundException;
import com.luana.projetoimoveis.repository.ImovelRepository;
import com.luana.projetoimoveis.repository.PrecoRepository;
import com.luana.projetoimoveis.repository.VendaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

@Service
public class VendaService {

	@Autowired
	private VendaRepository repository;

	@Autowired
	private ImovelRepository imovelRepository;

	@Autowired
	private PrecoRepository precoRepository;

	public List<Venda> findAll() {
		return repository.findAll();
	}

	public Venda findById(Integer id) {
		Optional<Venda> obj = repository.findById(id);
		return obj.orElseThrow(() -> new ResourceNotFoundException(id));
	}

	public Venda insert(Venda obj) {

		validarVenda(obj);

		obj = calculaValorTotalDoImovel(obj);
		
		return repository.save(obj);
	}

	private void validarVenda(Venda obj) {
		if(obj.getDataVenda().after(new Date())) {
			throw new BusinessException("A data da venda não pode ser superior a data atual.");
		}
		
		if(repository.imovelJaFoiVendido(obj.getIdImovel())) {
			throw new BusinessException("Este imóvel não está mais disponível para venda. Por favor escolha um outro imóvel de nossa lista.");
		}
	}

	private Venda calculaValorTotalDoImovel(Venda obj) {
		// buscar imovel pelo ID
		Optional<Imovel> imovelObj = imovelRepository.findById(obj.getIdImovel());
		Imovel imovel = imovelObj.get();

		// buscar preço por tipo e material do imovel
		Preco preco = precoRepository.buscaPrecoPorTipoImovelETipoMaterial(imovel.getTipo(), imovel.getMaterial());

		if (preco == null) {
			throw new BusinessException("Não existe preço cadastrado para o tipo de imóvel "
					+ imovel.getTipo().getDescricao() + " e material " + imovel.getMaterial().getDescricao());
		}

		// multiplicar pela metragem do imovel
		Double valorTotalCalculado = preco.getPrecoMetroQuad() * imovel.getTamanho(); 
		
		//Aplica o valor total para a venda a ser cadastrada
		obj.setValorTotal(valorTotalCalculado);
		
		return obj;
	}
	
	public void delete(Integer id) {
		try {
			repository.deleteById(id);
		} catch (EmptyResultDataAccessException e) {
			throw new ResourceNotFoundException(id);
		} catch (DataIntegrityViolationException e) {
			throw new DataBaseException(e.getMessage());
		}
	}

	public Venda update(Integer id, Venda obj) {
		try {

			if (!repository.existsById(id)) {
				throw new ResourceNotFoundException("A venda com o ID:" + id + " não existe.");
			}
			obj.setId(id);

			
			validarVenda(obj);
			
			obj = calculaValorTotalDoImovel(obj);

			return repository.save(obj);
		} catch (EntityNotFoundException e) {
			throw new ResourceNotFoundException(id);
		}
	}
	
	public List<Venda> listaVendasPorMesEAno(Integer mes, Integer ano){
		return repository.listaVendasPorMesEAno(mes, ano);
	}
}
