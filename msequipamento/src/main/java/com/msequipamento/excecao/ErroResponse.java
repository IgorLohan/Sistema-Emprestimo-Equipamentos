package com.msequipamento.excecao;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
		int status,
		String erro,
		Map<String, String> campos,
		LocalDateTime timestamp
) {

	public static ErroResponse of(int status, String erro) {
		return new ErroResponse(status, erro, null, LocalDateTime.now());
	}

	public static ErroResponse of(int status, String erro, Map<String, String> campos) {
		return new ErroResponse(status, erro, campos, LocalDateTime.now());
	}

}
