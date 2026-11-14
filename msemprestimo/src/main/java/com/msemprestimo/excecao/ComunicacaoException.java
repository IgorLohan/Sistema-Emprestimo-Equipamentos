package com.msemprestimo.excecao;

import org.springframework.http.HttpStatus;

public class ComunicacaoException extends RuntimeException {

	private final HttpStatus status;

	public ComunicacaoException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
