package com.msemprestimo.excecao;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
		return resposta(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflitoException.class)
	public ResponseEntity<ErroResponse> conflito(ConflitoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(ComunicacaoException.class)
	public ResponseEntity<ErroResponse> comunicacao(ComunicacaoException ex) {
		return resposta(ex.getStatus(), ex.getMessage());
	}

	@ExceptionHandler(RequisicaoInvalidaException.class)
	public ResponseEntity<ErroResponse> requisicaoInvalida(RequisicaoInvalidaException ex) {
		return resposta(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
		Map<String, String> campos = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> campos.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest()
				.body(ErroResponse.of(HttpStatus.BAD_REQUEST.value(), "Dados inválidos", campos));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErroResponse> identificadorInvalido(MethodArgumentTypeMismatchException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Identificador inválido");
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex) {
		return resposta(HttpStatus.CONFLICT, mensagemDuplicidade(ex));
	}

	private ResponseEntity<ErroResponse> resposta(HttpStatus status, String erro) {
		return ResponseEntity.status(status).body(ErroResponse.of(status.value(), erro));
	}

	private String mensagemDuplicidade(DataIntegrityViolationException ex) {
		String texto = ex.getMostSpecificCause().getMessage();
		if (texto != null && texto.toLowerCase(Locale.ROOT).contains("uk_emprestimo_equipamento_ativo")) {
			return "Já existe um empréstimo ativo para este equipamento";
		}
		return "Dados duplicados";
	}

}
