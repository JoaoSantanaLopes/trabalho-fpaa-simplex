import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * A SlackForm guarda o programa linear na "forma de folga", que e o jeito como
 * o livro CLRS representa o problema enquanto o Simplex trabalha nele.
 *
 * A ideia e dividir as variaveis em dois grupos: as basicas (que ficam isoladas
 * do lado esquerdo das equacoes) e as nao-basicas (que ficam do lado direito).
 * O sistema que representamos tem esta cara:
 *
 *     z    = v + (soma dos c[j] * x[j], para cada nao-basica j)
 *     x[i] = b[i] + (soma dos A[i][j] * x[j], para cada nao-basica j)
 *
 * onde v e a constante do objetivo, b[i] e o valor de cada basica quando as
 * nao-basicas valem zero, e A guarda os coeficientes.
 *
 * Um detalhe importante: guardamos a matriz A com o mesmo sinal que ela aparece
 * nas equacoes acima. O livro usa o sinal trocado, mas manter o sinal "natural"
 * deixa tudo mais facil de ler. So vale lembrar disso ao comparar com o livro.
 *
 * Usamos Mapas em vez de vetores porque cada variavel tem um indice fixo que
 * nunca muda, mesmo quando ela troca de lado durante o algoritmo. Assim o x3 e
 * sempre o x3, seja ele basico ou nao-basico num dado momento.
 */
public class SlackForm {

    // Indices das variaveis basicas (o conjunto B do livro).
    final Set<Integer> variaveisBasicas;

    // Indices das variaveis nao-basicas (o conjunto N do livro).
    final Set<Integer> variaveisNaoBasicas;

    // Para cada variavel basica, os coeficientes das nao-basicas na equacao dela.
    final Map<Integer, Map<Integer, Double>> matrizA;

    // Valor constante de cada variavel basica.
    final Map<Integer, Double> vetorB;

    // Coeficiente de cada variavel nao-basica na funcao objetivo.
    final Map<Integer, Double> vetorC;

    // Constante da funcao objetivo (o "v" do livro).
    double constanteObjetivo;

    // Quantas variaveis o problema original tinha (x0 ate x(n-1)). Guardamos
    // isso so para saber, no fim, quais variaveis mostrar na resposta.
    final int numeroDeVariaveis;

    /**
     * Monta a forma de folga a partir de um problema de maximizacao escrito na
     * forma padrao:
     *
     *     maximizar   soma dos c[j] * x[j]
     *     sujeito a   soma dos coef[i][j] * x[j] <= limite[i]
     *                 todas as variaveis >= 0
     *
     * As variaveis originais ganham os indices 0 a n-1 e comecam nao-basicas.
     * Cada restricao ganha uma variavel de folga (indice n+i) que comeca basica.
     * Essa e a solucao inicial: todas as originais valem zero e cada folga
     * absorve a sobra da sua restricao.
     */
    public SlackForm(double[][] coeficientesRestricoes,
                     double[] limites,
                     double[] coeficientesObjetivo) {

        this.numeroDeVariaveis = coeficientesObjetivo.length;
        int numeroDeRestricoes = limites.length;

        // Usamos TreeSet para os indices ficarem sempre em ordem. Isso deixa a
        // impressao mais organizada e ja faz a escolha pela menor variavel sair
        // de graca (que e a regra de Bland, usada para evitar loops infinitos).
        this.variaveisBasicas = new TreeSet<>();
        this.variaveisNaoBasicas = new TreeSet<>();
        this.matrizA = new HashMap<>();
        this.vetorB = new HashMap<>();
        this.vetorC = new HashMap<>();
        this.constanteObjetivo = 0.0;

        // As variaveis originais comecam todas como nao-basicas.
        for (int j = 0; j < numeroDeVariaveis; j++) {
            variaveisNaoBasicas.add(j);
            vetorC.put(j, coeficientesObjetivo[j]);
        }

        // Cada restricao vira uma variavel de folga basica.
        for (int i = 0; i < numeroDeRestricoes; i++) {
            int indiceFolga = numeroDeVariaveis + i;
            variaveisBasicas.add(indiceFolga);
            vetorB.put(indiceFolga, limites[i]);

            // Os coeficientes entram com o sinal trocado (ver explicacao no topo).
            Map<Integer, Double> linha = new HashMap<>();
            for (int j = 0; j < numeroDeVariaveis; j++) {
                linha.put(j, -coeficientesRestricoes[i][j]);
            }
            matrizA.put(indiceFolga, linha);
        }
    }

    /**
     * Mostra a forma de folga atual na tela, no mesmo formato das equacoes.
     * E o que deixa o passo a passo visivel durante a execucao.
     */
    public void imprimir() {
        StringBuilder objetivo = new StringBuilder("z = " + formatar(constanteObjetivo));
        for (int j : variaveisNaoBasicas) {
            double coef = vetorC.get(j);
            if (coef != 0.0) {
                objetivo.append("  ").append(comSinal(coef)).append(" x").append(j);
            }
        }
        System.out.println(objetivo);

        for (int i : variaveisBasicas) {
            StringBuilder linha = new StringBuilder("x" + i + " = " + formatar(vetorB.get(i)));
            Map<Integer, Double> coeficientes = matrizA.get(i);
            for (int j : variaveisNaoBasicas) {
                double coef = coeficientes.getOrDefault(j, 0.0);
                if (coef != 0.0) {
                    linha.append("  ").append(comSinal(coef)).append(" x").append(j);
                }
            }
            System.out.println(linha);
        }
    }

    // Tira o ".0" dos numeros inteiros so para a leitura ficar mais limpa.
    private String formatar(double valor) {
        if (valor == Math.floor(valor) && !Double.isInfinite(valor)) {
            return String.valueOf((long) valor);
        }
        return String.valueOf(valor);
    }

    // Coloca o "+" ou "-" na frente do numero para montar as equacoes direitinho.
    private String comSinal(double valor) {
        return valor >= 0 ? "+ " + formatar(valor) : "- " + formatar(-valor);
    }
}