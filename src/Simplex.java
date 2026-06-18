import java.util.HashMap;
import java.util.Map;

/**
 * A classe Simplex e o coracao do projeto: e aqui que o algoritmo do livro CLRS
 * acontece. A ideia geral e simples de descrever: o problema linear desenha uma
 * regiao no espaco (um poliedro), e a solucao otima esta sempre num dos cantos
 * dessa regiao. O Simplex comeca num canto e vai andando de canto em canto,
 * sempre na direcao que aumenta o objetivo, ate nao dar mais para melhorar.
 *
 * O algoritmo aparece dividido em tres metodos, iguais aos tres procedimentos
 * do livro:
 *   - pivo        -> PIVOT          (troca uma variavel da base por outra)
 *   - resolver    -> SIMPLEX        (o laco principal que anda de canto em canto)
 *   - inicializar -> INITIALIZE-SIMPLEX (acha um ponto de partida valido)
 *
 * Mantivemos os nomes "e" e "l" como no livro: "e" e a variavel que entra na
 * base (de entering) e "l" e a que sai (de leaving).
 */
public class Simplex {

    // Margem de tolerancia para tratar numeros muito proximos de zero como zero.
    // Sem isso, um arredondamento tipo 0.0000001 poderia enganar o algoritmo e
    // fazer ele nunca parar.
    private static final double EPSILON = 1e-9;

    // Liga ou desliga a impressao do passo a passo durante a execucao.
    private final boolean mostrarPassos;

    public Simplex(boolean mostrarPassos) {
        this.mostrarPassos = mostrarPassos;
    }

    /**
     * SIMPLEX do livro: o laco principal.
     *
     * Primeiro garante que temos um ponto de partida valido (se a solucao
     * inicial nao serve, chama o inicializar). Depois fica repetindo: escolhe
     * uma variavel que melhora o objetivo, descobre o quanto ela pode crescer e
     * faz o pivo. Quando nenhuma variavel melhora mais, chegamos ao otimo.
     *
     * Devolve os valores otimos das variaveis originais (x0 ate x(n-1)).
     */
    public double[] resolver(SlackForm formaDeFolga) {
        // Se algum b for negativo, a solucao inicial nao e valida e precisamos
        // primeiro arrumar um ponto de partida com o problema auxiliar.
        boolean precisaInicializar = formaDeFolga.vetorB.values().stream()
                .anyMatch(valor -> valor < -EPSILON);
        if (precisaInicializar) {
            inicializar(formaDeFolga);
        }

        // Roda o laco principal ate nao poder melhorar mais.
        iterarAteOtimo(formaDeFolga, true);

        // Monta a resposta: cada variavel basica vale seu b, cada nao-basica vale 0.
        double[] solucao = new double[formaDeFolga.numeroDeVariaveis];
        for (int j = 0; j < formaDeFolga.numeroDeVariaveis; j++) {
            solucao[j] = formaDeFolga.variaveisBasicas.contains(j)
                    ? formaDeFolga.vetorB.get(j)
                    : 0.0;
        }
        return solucao;
    }

    /**
     * Roda o laco de pivos ate o objetivo nao poder mais aumentar.
     *
     * A cada volta:
     *   1) acha uma variavel "e" que ainda melhora o objetivo;
     *   2) usa o teste da razao para ver qual restricao limita primeiro o
     *      crescimento de "e" (isso define a variavel "l" que sai);
     *   3) faz o pivo trocando e por l.
     *
     * O parametro acusarIlimitado diz o que fazer quando nenhuma restricao
     * limita o crescimento de "e": no problema de verdade isso significa que o
     * objetivo cresce infinito (erro de ilimitado); no problema auxiliar isso
     * nao deve acontecer, entao apenas paramos por seguranca.
     */
    private void iterarAteOtimo(SlackForm formaDeFolga, boolean acusarIlimitado) {
        int e = variavelQuePodeMelhorar(formaDeFolga);
        while (e != -1) {
            int l = escolherVariavelQueSai(formaDeFolga, e);

            if (l == -1) {
                if (acusarIlimitado) {
                    throw new IllegalStateException("O programa linear e ilimitado.");
                }
                return;
            }

            pivo(formaDeFolga, e, l);
            e = variavelQuePodeMelhorar(formaDeFolga);
        }
    }

    /**
     * Procura uma variavel nao-basica com coeficiente positivo no objetivo.
     * Se existe, aumentar essa variavel aumenta z, ou seja, ainda da para
     * melhorar. Devolve o indice dela, ou -1 se ja estamos no otimo.
     *
     * Como o conjunto e ordenado, sempre pegamos a de menor indice (regra de
     * Bland), o que evita ciclos infinitos.
     */
    private int variavelQuePodeMelhorar(SlackForm formaDeFolga) {
        for (int j : formaDeFolga.variaveisNaoBasicas) {
            if (formaDeFolga.vetorC.get(j) > EPSILON) {
                return j;
            }
        }
        return -1;
    }

    /**
     * Teste da razao: descobre qual variavel basica vai sair quando "e" entrar.
     *
     * Aumentar "e" diminui as basicas cujos coeficientes sao negativos. A
     * primeira que chegaria a zero e a que limita o crescimento, entao ela e a
     * escolhida para sair. Devolve o indice dela, ou -1 se nada limita "e"
     * (sinal de problema ilimitado).
     */
    private int escolherVariavelQueSai(SlackForm formaDeFolga, int e) {
        double limiteMaisApertado = Double.POSITIVE_INFINITY;
        int l = -1;
        for (int i : formaDeFolga.variaveisBasicas) {
            double coefDeE = formaDeFolga.matrizA.get(i).get(e);
            if (coefDeE < -EPSILON) {
                double limite = -formaDeFolga.vetorB.get(i) / coefDeE;
                if (limite < limiteMaisApertado) {
                    limiteMaisApertado = limite;
                    l = i;
                }
            }
        }
        return l;
    }

    /**
     * PIVOT do livro: troca a variavel "e" (que entra) pela "l" (que sai).
     *
     * Na pratica, pegamos a equacao de "l", isolamos "e" nela, e substituimos
     * essa nova expressao de "e" em todas as outras equacoes e no objetivo. No
     * fim, "e" virou basica e "l" virou nao-basica.
     */
    private void pivo(SlackForm formaDeFolga, int e, int l) {
        if (mostrarPassos) {
            System.out.println("\n==> Pivo: x" + e + " entra na base, x" + l + " sai");
        }

        Map<Integer, Map<Integer, Double>> A = formaDeFolga.matrizA;
        Map<Integer, Double> b = formaDeFolga.vetorB;
        Map<Integer, Double> c = formaDeFolga.vetorC;

        // O coeficiente de "e" na equacao de "l" e o nosso pivo: tudo na nova
        // equacao de "e" sera dividido por ele.
        double coeficientePivo = A.get(l).get(e);

        // Monta a nova equacao da variavel que entra, isolando "e".
        b.put(e, -b.get(l) / coeficientePivo);
        Map<Integer, Double> novaLinhaE = new HashMap<>();
        for (Map.Entry<Integer, Double> termo : A.get(l).entrySet()) {
            if (termo.getKey() != e) {
                novaLinhaE.put(termo.getKey(), -termo.getValue() / coeficientePivo);
            }
        }
        // A variavel "l" agora aparece do lado direito da equacao de "e".
        novaLinhaE.put(l, 1.0 / coeficientePivo);
        A.put(e, novaLinhaE);
        A.remove(l);

        // Substitui a expressao de "e" em todas as outras equacoes basicas.
        for (int i : A.keySet()) {
            if (i == e) {
                continue;
            }
            Map<Integer, Double> linhaI = A.get(i);
            double coefDeE = linhaI.get(e);

            b.put(i, b.get(i) + coefDeE * b.get(e));
            for (Map.Entry<Integer, Double> termo : novaLinhaE.entrySet()) {
                if (termo.getKey() != e) {
                    double atual = linhaI.getOrDefault(termo.getKey(), 0.0);
                    linhaI.put(termo.getKey(), atual + coefDeE * termo.getValue());
                }
            }
            linhaI.put(l, coefDeE * novaLinhaE.get(l));
            linhaI.remove(e);
        }

        // Substitui a expressao de "e" tambem na funcao objetivo.
        double coefDeEnoObjetivo = c.get(e);
        formaDeFolga.constanteObjetivo += coefDeEnoObjetivo * b.get(e);
        for (Map.Entry<Integer, Double> termo : novaLinhaE.entrySet()) {
            if (termo.getKey() != e) {
                double atual = c.getOrDefault(termo.getKey(), 0.0);
                c.put(termo.getKey(), atual + coefDeEnoObjetivo * termo.getValue());
            }
        }
        c.put(l, coefDeEnoObjetivo * novaLinhaE.get(l));
        c.remove(e);

        // Atualiza quem e basico e quem e nao-basico: "e" entra, "l" sai.
        formaDeFolga.variaveisNaoBasicas.remove(e);
        formaDeFolga.variaveisBasicas.remove(l);
        formaDeFolga.variaveisBasicas.add(e);
        formaDeFolga.variaveisNaoBasicas.add(l);

        if (mostrarPassos) {
            formaDeFolga.imprimir();
        }
    }

    /**
     * INITIALIZE-SIMPLEX do livro: arruma um ponto de partida valido.
     *
     * Quando a solucao inicial nao serve (tem b negativo), nao da para comecar
     * o Simplex direto. O truque e montar um problema auxiliar: a gente cria uma
     * variavel artificial e troca o objetivo por "minimizar essa artificial".
     * Se conseguimos zera-la, achamos um ponto valido para o problema de
     * verdade; se ela teima em ficar positiva, o problema original nao tem
     * solucao. Esta e a estrategia de duas fases, numa versao didatica.
     */
    private void inicializar(SlackForm formaDeFolga) {
        if (mostrarPassos) {
            System.out.println("\nSolucao inicial inviavel. Montando problema auxiliar...");
        }

        Map<Integer, Map<Integer, Double>> A = formaDeFolga.matrizA;
        Map<Integer, Double> b = formaDeFolga.vetorB;
        Map<Integer, Double> c = formaDeFolga.vetorC;

        // Guarda o objetivo original para colocar de volta no fim da fase 1.
        Map<Integer, Double> objetivoOriginal = new HashMap<>(c);
        double constanteOriginal = formaDeFolga.constanteObjetivo;

        // A variavel artificial recebe um indice acima de todos os existentes.
        int artificial = Math.max(maiorIndice(formaDeFolga.variaveisBasicas),
                                   maiorIndice(formaDeFolga.variaveisNaoBasicas)) + 1;

        // Coloca a artificial em todas as equacoes e define o objetivo da fase 1
        // como maximizar -artificial (ou seja, empurrar ela para zero).
        for (int i : formaDeFolga.variaveisBasicas) {
            A.get(i).put(artificial, 1.0);
        }
        formaDeFolga.variaveisNaoBasicas.add(artificial);
        c.clear();
        for (int j : formaDeFolga.variaveisNaoBasicas) {
            c.put(j, 0.0);
        }
        c.put(artificial, -1.0);
        formaDeFolga.constanteObjetivo = 0.0;

        // Primeiro pivo: a basica mais negativa sai e a artificial entra. Isso
        // ja deixa a solucao valida para tocar o laco normal.
        int l = variavelMaisNegativa(formaDeFolga);
        if (mostrarPassos) {
            System.out.println("Variavel artificial adicionada: x" + artificial);
            formaDeFolga.imprimir();
        }
        pivo(formaDeFolga, artificial, l);

        // Resolve o problema auxiliar normalmente.
        iterarAteOtimo(formaDeFolga, false);

        // Se a artificial ficou na base com valor positivo, nao ha solucao.
        boolean artificialAindaBasica = formaDeFolga.variaveisBasicas.contains(artificial);
        if (artificialAindaBasica && b.get(artificial) > EPSILON) {
            throw new IllegalStateException("O programa linear nao tem solucao viavel.");
        }

        // Se a artificial ficou basica mas valendo zero, tiramos ela com um pivo.
        if (artificialAindaBasica) {
            for (int j : A.get(artificial).keySet()) {
                if (Math.abs(A.get(artificial).get(j)) > EPSILON) {
                    pivo(formaDeFolga, j, artificial);
                    break;
                }
            }
        }

        // Remove a artificial de toda a estrutura.
        formaDeFolga.variaveisNaoBasicas.remove(artificial);
        c.remove(artificial);
        for (int i : formaDeFolga.variaveisBasicas) {
            A.get(i).remove(artificial);
        }

        restaurarObjetivoOriginal(formaDeFolga, objetivoOriginal, constanteOriginal);

        if (mostrarPassos) {
            System.out.println("\nProblema auxiliar resolvido. Forma de folga viavel:");
            formaDeFolga.imprimir();
        }
    }

    /**
     * Reescreve o objetivo original em funcao das nao-basicas atuais. Como
     * algumas variaveis originais podem ter virado basicas durante a fase 1,
     * cada uma delas precisa ser substituida pela sua equacao.
     */
    private void restaurarObjetivoOriginal(SlackForm formaDeFolga,
                                           Map<Integer, Double> objetivoOriginal,
                                           double constanteOriginal) {
        Map<Integer, Double> c = formaDeFolga.vetorC;
        c.clear();
        formaDeFolga.constanteObjetivo = constanteOriginal;
        for (int j : formaDeFolga.variaveisNaoBasicas) {
            c.put(j, 0.0);
        }

        for (Map.Entry<Integer, Double> termo : objetivoOriginal.entrySet()) {
            int variavel = termo.getKey();
            double coef = termo.getValue();
            if (formaDeFolga.variaveisNaoBasicas.contains(variavel)) {
                // Continua nao-basica: o coeficiente entra direto.
                c.put(variavel, c.get(variavel) + coef);
            } else if (formaDeFolga.variaveisBasicas.contains(variavel)) {
                // Virou basica: troca pela sua equacao.
                formaDeFolga.constanteObjetivo += coef * formaDeFolga.vetorB.get(variavel);
                for (Map.Entry<Integer, Double> t : formaDeFolga.matrizA.get(variavel).entrySet()) {
                    c.put(t.getKey(), c.getOrDefault(t.getKey(), 0.0) + coef * t.getValue());
                }
            }
        }
    }

    // Devolve o maior indice de um conjunto (0 se estiver vazio).
    private int maiorIndice(Iterable<Integer> indices) {
        int maior = 0;
        for (int indice : indices) {
            maior = Math.max(maior, indice);
        }
        return maior;
    }

    // Acha a variavel basica com o b mais negativo (a "mais inviavel").
    private int variavelMaisNegativa(SlackForm formaDeFolga) {
        double menor = 0.0;
        int escolhida = -1;
        for (int i : formaDeFolga.variaveisBasicas) {
            if (formaDeFolga.vetorB.get(i) < menor) {
                menor = formaDeFolga.vetorB.get(i);
                escolhida = i;
            }
        }
        return escolhida;
    }
}