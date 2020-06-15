package com.pgioseffi.cpf.gerador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class GeradorCPF {

	private static final Collection<Estado> SIGLAS_ESTADOS = EnumSet.allOf(Estado.class);

	private static final Random GERADOR_DIGITOS = new Random();

	public GeradorCPF() {
		throw new AssertionError("Nada de tentar instanciar esta classe"); //$NON-NLS-1$
	}

	private enum Estado {
		DF("DF", 1), //$NON-NLS-1$
		GO("GO", 1), //$NON-NLS-1$
		MS("MS", 1), //$NON-NLS-1$
		MT("MT", 1), //$NON-NLS-1$
		TO("TO", 1), //$NON-NLS-1$
		AC("AC", 2), //$NON-NLS-1$
		AM("AM", 2), //$NON-NLS-1$
		AP("AP", 2), //$NON-NLS-1$
		PA("PA", 2), //$NON-NLS-1$
		RO("RO", 2), //$NON-NLS-1$
		RR("RR", 2), //$NON-NLS-1$
		CE("CE", 3), //$NON-NLS-1$
		MA("MA", 3), //$NON-NLS-1$
		PI("PI", 3), //$NON-NLS-1$
		AL("AL", 4), //$NON-NLS-1$
		PB("PB", 4), //$NON-NLS-1$
		PE("PE", 4), //$NON-NLS-1$
		RN("RN", 4), //$NON-NLS-1$
		BA("BA", 5), //$NON-NLS-1$
		SE("SE", 5), //$NON-NLS-1$
		MG("MG", 6), //$NON-NLS-1$
		ES("ES", 7), //$NON-NLS-1$
		RJ("RJ", 7), //$NON-NLS-1$
		SP("SP", 8), //$NON-NLS-1$
		PR("PR", 9), //$NON-NLS-1$
		SC("SC", 9), //$NON-NLS-1$
		RS("RS", 0); //$NON-NLS-1$

		private final String sigla;
		private final int regiaoFiscal;

		Estado(final String siglaParam, final int regiaoFiscalParam) {
			this.sigla = siglaParam;
			this.regiaoFiscal = regiaoFiscalParam;
		}

		String getSigla() {
			return this.sigla;
		}

		int getRegiaoFiscal() {
			return this.regiaoFiscal;
		}

		static Optional<Estado> retornarEstadoPorSigla(final String sigla) {
			return Arrays.stream(Estado.values()).filter(e -> e.sigla.equalsIgnoreCase(sigla)).findFirst();
		}
	}

	public static void main(final String[] args) {
		System.out.println("Escolha seu estado dentre a lista de opções abaixo:\n" //$NON-NLS-1$
				+ GeradorCPF.SIGLAS_ESTADOS.stream().map(Estado::getSigla).sorted().collect(Collectors.joining(", "))); //$NON-NLS-1$

//		GeradorCPF.SIGLAS_ESTADOS.stream().map(Estado::getSigla).sorted() .forEach(System.out::println);
		System.out.println('\n');

		try (Scanner in = new Scanner(System.in)) {
			final String sigla = in.nextLine();
			final Optional<Estado> estado = Estado.retornarEstadoPorSigla(sigla);
			if (estado.isPresent()) {
				final int maximo = 3_000_000;

				System.out.println(
						String.format("Digite a quantidade de CPFs que deseja gerar, sendo a quantidade máxima %d.", //$NON-NLS-1$
								Integer.valueOf(maximo)));

				final int limite = in.nextInt(10);
				if (limite < 1 || limite > maximo) {
					System.out.println("Quantidade inválida de CPF a ser gerado informado. Abortando..."); //$NON-NLS-1$
				} else {
					GeradorCPF.escreverResultado(estado.get(), limite);
				}
			} else {
				System.out.println("Estado inválido. Abortando..."); //$NON-NLS-1$
			}
		}
	}

	private static void escreverResultado(final Estado estado, final int limite) {
		final long inicio = System.currentTimeMillis();
		System.out.println("Arquivo sendo gerado."); //$NON-NLS-1$
		final StringBuilder cpf = new StringBuilder(16);
		final StringBuilder linha = new StringBuilder(28 * limite).append("CPF:\t\t\tCPF Formatado:\n"); //$NON-NLS-1$
		final Set<String> cpfs = new HashSet<>(limite);

		while (cpfs.size() < limite) {
			int multiplicadorPrimeiroDigito = 10;
			int multiplicadorSegundoDigito = 11;
			int totalPrimeiroDigito = 0;
			int totalSegundoDigito = 0;
			int somatorio = 0;

			for (int j = 0; j < 8; j++) {
				final int digito = GeradorCPF.GERADOR_DIGITOS.nextInt(10);
				cpf.append(Character.forDigit(digito, 10));
				totalPrimeiroDigito += multiplicadorPrimeiroDigito * digito;
				totalSegundoDigito += multiplicadorSegundoDigito * digito;
				multiplicadorPrimeiroDigito--;
				multiplicadorSegundoDigito--;
				somatorio += digito;
			}

			final int regiaoFiscal = estado.getRegiaoFiscal();
			final int primeiroDigito = GeradorCPF
					.retornarDigito(totalPrimeiroDigito + regiaoFiscal * multiplicadorPrimeiroDigito);
			final int segundoDigito = totalSegundoDigito + regiaoFiscal * multiplicadorSegundoDigito
					+ primeiroDigito * (multiplicadorSegundoDigito - 1);

			cpf.append(Character.forDigit(regiaoFiscal, 10)).append(Character.forDigit(primeiroDigito, 10))
					.append(Character.forDigit(GeradorCPF.retornarDigito(segundoDigito), 10));

			somatorio += regiaoFiscal + primeiroDigito + segundoDigito;

			final String cpfStr = cpf.toString();
			if (somatorio > 9 && somatorio < 89 && GeradorCPF.isSomatorioModuloOnzeValido(somatorio)
					&& GeradorCPF.isCPFValido(cpfStr) && cpfs.add(cpfStr)) {
				linha.append(cpf).append("\t\t").append(cpf.insert(3, '.').insert(7, '.').insert(11, '-')).append('\n'); //$NON-NLS-1$

				System.out.println(String.format("Quantidade CPFs gerados: %d.", Integer.valueOf(cpfs.size()))); //$NON-NLS-1$
			}

			cpf.setLength(0);
		}

		try {
			final String siglaEstado = estado.getSigla();

			Files.write(Paths.get(String.format("./resultado_%s.txt", siglaEstado)), //$NON-NLS-1$
					linha.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);

			System.out.println(String.format("Arquivo de CPFs do estado de %s gerado com sucesso em %.2f segundos.", //$NON-NLS-1$
					siglaEstado, Double.valueOf((System.currentTimeMillis() - inicio) / (double) 1000)));
		} catch (final IOException e) {
			System.err.println("Falha na geração do arquivo."); //$NON-NLS-1$
			e.printStackTrace(System.err);
		}
	}

	private static boolean isSomatorioModuloOnzeValido(final int somatorio) {
		final int somatorioModuloOnze = somatorio % 11;
		return somatorioModuloOnze == 0 || somatorioModuloOnze == 1 || somatorioModuloOnze == 10;
	}

	private static boolean isCPFValido(final CharSequence cpf) {
		switch (cpf.toString()) {
			case "00000000000":
			case "11111111111":
			case "22222222222":
			case "33333333333":
			case "44444444444":
			case "55555555555":
			case "66666666666":
			case "77777777777":
			case "88888888888":
			case "99999999999":
				return false;
			default:
				return true;
		}
	}

	private static int retornarDigito(final int total) {
		final int resto = total % 11;
		return resto < 2 ? 0 : 11 - resto;
	}
}