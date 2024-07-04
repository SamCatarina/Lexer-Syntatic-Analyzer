import java.io.File

fun main() {
    val lexer = Lexer()
    val parser = Parser(lexer)

    try {
        val input = File("C:\\Users\\Catarina\\IdeaProjects\\untitled1\\src\\teste.txt").readText()
        println("Conteúdo do arquivo de entrada:\n$input")
        parser.parse(input)
        println("Análise sintática concluída.")
    } catch (e: Exception) {
        println("Erro durante a análise sintática: ${e.message}")
    }
}
