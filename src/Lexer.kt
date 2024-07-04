import java.util.regex.Pattern

enum class TokenType {
    COMMENT, STRING, RESERVED_WORD, CHARACTER, IDENTIFIER, FLOAT, INT, RELATIONAL_SYMBOLS, SPECIAL_SYMBOLS
}

data class Token(val type: TokenType, val lexeme: String, val lineNumber: Int)

class Lexer {
    fun tokenize(input: String): List<Token> {
        var currentLineNumber = 1
        var remainingInput = input.trim()

        val patterns = listOf(
            Pair(TokenType.COMMENT, "--.*|(?s)-\\{.*?-\\}"),
            Pair(TokenType.STRING, "\".*?\""),
            Pair(
                TokenType.RESERVED_WORD,
                "\\b(char|string|inpt!!|int|float|rational|program|if|else|while|input|print|return)\\b"
            ),
            Pair(TokenType.CHARACTER, "'.'"),
            Pair(TokenType.IDENTIFIER, "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b"),
            Pair(TokenType.FLOAT, "-?\\d+\\.\\d+(?!\\w)"),
            Pair(TokenType.INT, "-?\\d+(?!\\w)"),
            Pair(TokenType.RELATIONAL_SYMBOLS, "<=|>=|<>|<|>|!=|!"),
            Pair(
                TokenType.SPECIAL_SYMBOLS,
                "\\,|\\;|\\(|\\)|\\[|\\]|\\{|\\}|\\=|\\+|\\-|\\*|\\/|\\%|\\<|\\>|\\&|\\||\\~|\\!"
            )
        )

        val tokens = mutableListOf<Token>()

        while (remainingInput.isNotEmpty()) {
            var matched = false

            for ((tokenType, pattern) in patterns) {
                val compiledPattern = Pattern.compile("^($pattern)")
                val matcher = compiledPattern.matcher(remainingInput)

                if (matcher.find()) {
                    val lexeme = matcher.group().trim()
                    val newLines = lexeme.count { it == '\n' }
                    if (tokenType != TokenType.COMMENT) {
                        tokens.add(Token(tokenType, lexeme, currentLineNumber))
                    }
                    remainingInput = remainingInput.drop(lexeme.length).trimStart()
                    currentLineNumber += newLines
                    matched = true
                    break
                }
            }

            if (!matched) {

                val invalidToken = remainingInput.takeWhile { !it.isWhitespace() && it != ';' && it != ',' }
                throw SyntaxErrorException("Erro léxico na linha $currentLineNumber: token inválido encontrado '$invalidToken'.")
            }

            currentLineNumber += remainingInput.takeWhile { it == '\n' }.length
        }
        print(tokens)
        return tokens
    }
}

class SyntaxErrorException(message: String) : Exception(message)
