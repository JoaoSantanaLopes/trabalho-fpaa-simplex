import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Main e o ponto de entrada do programa. Ela e proposital e bem curta: sua
 * unica responsabilidade e ler os dados do programa linear, montar a SlackForm
 * e pedir para a classe Simplex resolver. Toda a logica do algoritmo vive na
 * classe Simplex; toda a representacao do problema vive na classe SlackForm.
 *
 * FORMATO DA ENTRADA (problema de MAXIMIZACAO em forma padrao):
 *   - 1a linha: dois inteiros n e m (numero de variaveis e numero de restricoes)
 *   - m linhas: a matriz de coeficientes das restricoes (n numeros por linha)
 *   - 1 linha : o vetor de limites b (m numeros) -- lado direito das restricoes <=
 *   - 1 linha : o vetor c da funcao objetivo (n numeros)
 *
 * Exemplo:
 *   maximizar   x0 + 2 x1 + 0.5 x2
 *   sujeito a   x0 + x1 + x2 <= 5
 *               x0           <= 3
 *                    x1      <= 1
 *                         x2 <= 4
 *   entrada:
 *               3 4
 *               1 1 1
 *               1 0 0
 *               0 1 0
 *               0 0 1
 *               5 3 1 4
 *               1 2 0.5
 *
 * COMO USAR:
 *   - Sem argumentos: roda um exemplo embutido (bom para teste rapido).
 *   - Com um caminho de arquivo: le o problema desse arquivo.
 *   - Passe "-no" como segundo argumento para nao imprimir o passo a passo.
 */
public class Main {

    // Exemplo usado quando o programa roda sem nenhum arquivo.
    private static final String EXEMPLO =
            "3 4\n" +
            "1 1 1\n" +
            "1 0 0\n" +
            "0 1 0\n" +
            "0 0 1\n" +
            "5 3 1 4\n" +
            "1 2 0.5\n";

    public static void main(String[] args) {
        try {
            String entrada;
            boolean mostrarPassos = true;

            if (args.length == 0) {
                System.out.println("Nenhum arquivo informado. Rodando o exemplo embutido.\n");
                entrada = EXEMPLO;
            } else {
                entrada = new String(Files.readAllBytes(Paths.get(args[0])));
                // O segundo argumento "-no" desliga a impressao do passo a passo.
                if (args.length >= 2 && args[1].startsWith("-n")) {
                    mostrarPassos = false;
                }
            }

            // Le os dados e monta a forma de folga.
            SlackForm formaDeFolga = lerEntrada(entrada);

            System.out.println("Forma de folga inicial:");
            formaDeFolga.imprimir();

            // Resolve o problema.
            Simplex simplex = new Simplex(mostrarPassos);
            double[] solucao = simplex.resolver(formaDeFolga);

            // Mostra o resultado final.
            System.out.println("\n========== SOLUCAO OTIMA ==========");
            for (int j = 0; j < solucao.length; j++) {
                System.out.println("x" + j + " = " + formatar(solucao[j]));
            }
            System.out.println("z = " + formatar(formaDeFolga.constanteObjetivo));

        } catch (IOException erro) {
            System.out.println("Nao foi possivel ler o arquivo: " + erro.getMessage());
        } catch (IllegalStateException erro) {
            // Cai aqui quando o problema e ilimitado ou nao tem solucao.
            System.out.println("\n" + erro.getMessage());
        }
    }


    /**
     * Le o texto de entrada no formato descrito la em cima e ja devolve a
     * SlackForm pronta. A leitura e direta: numeros separados por espaco ou
     * quebra de linha, na ordem combinada.
     */
    private static SlackForm lerEntrada(String texto) {
        Scanner scanner = new Scanner(texto);

        int numeroDeVariaveis = scanner.nextInt();
        int numeroDeRestricoes = scanner.nextInt();

        double[][] coeficientesRestricoes = new double[numeroDeRestricoes][numeroDeVariaveis];
        double[] limites = new double[numeroDeRestricoes];
        double[] coeficientesObjetivo = new double[numeroDeVariaveis];

        // Le a matriz de coeficientes das restricoes.
        for (int i = 0; i < numeroDeRestricoes; i++) {
            for (int j = 0; j < numeroDeVariaveis; j++) {
                coeficientesRestricoes[i][j] = scanner.nextDouble();
            }
        }

        // Le o vetor de limites b.
        for (int i = 0; i < numeroDeRestricoes; i++) {
            limites[i] = scanner.nextDouble();
        }

        // Le o vetor c da funcao objetivo.
        for (int j = 0; j < numeroDeVariaveis; j++) {
            coeficientesObjetivo[j] = scanner.nextDouble();
        }

        scanner.close();
        return new SlackForm(coeficientesRestricoes, limites, coeficientesObjetivo);
    }


    // Formata um numero removendo o ".0" quando for inteiro, para a leitura ser mais limpa.
    private static String formatar(double valor) {
        if (valor == Math.floor(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }
}