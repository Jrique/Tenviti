package br.com.tenviti.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import tenviti.composeapp.generated.resources.Res
import tenviti.composeapp.generated.resources.inter_medium
import tenviti.composeapp.generated.resources.inter_regular
import tenviti.composeapp.generated.resources.inter_semibold
import tenviti.composeapp.generated.resources.space_grotesk_bold
import tenviti.composeapp.generated.resources.space_grotesk_medium
import tenviti.composeapp.generated.resources.space_grotesk_semibold

/** Par de cores para etiquetas e avisos: fundo suave, texto escuro e uma cor cheia para barras e pontos. */
@Immutable
data class CorDeStatus(val fundo: Color, val texto: Color, val cheia: Color)

/**
 * Paleta Tenviti "petróleo e coral". Base petróleo para menu e ações; coral só como acento
 * (o "TI", novidades e um destaque por tela); status com cores próprias, separadas da marca.
 */
@Immutable
data class CoresTenviti(
    val escuro: Boolean,
    val fundo: Color,
    val superficie: Color,
    val superficieAlt: Color,
    val borda: Color,
    val bordaSuave: Color,
    val texto: Color,
    val textoSuave: Color,
    val base: Color,
    val sobreBase: Color,
    val baseForte: Color,
    val baseSuave: Color,
    val sobreBaseSuave: Color,
    val acento: Color,
    val acentoTexto: Color,
    val acentoSuave: Color,
    val sobreAcentoSuave: Color,
    /** Cartão em destaque (ex.: "Vendido hoje"). */
    val destaque: Color,
    val sobreDestaque: Color,
    val sobreDestaqueSuave: Color,
    // Menu lateral: sempre escuro, nos dois modos
    val menuFundo: Color,
    val menuCartao: Color,
    val menuBorda: Color,
    val menuItemAtivo: Color,
    val menuTexto: Color,
    val menuTextoAtivo: Color,
    val menuTextoSuave: Color,
    val menuAcento: Color,
    val menuVerde: Color,
    // Status
    val info: CorDeStatus,
    val sucesso: CorDeStatus,
    val aviso: CorDeStatus,
    val erro: CorDeStatus,
    val neutro: CorDeStatus,
)

val CoresClaras = CoresTenviti(
    escuro = false,
    fundo = Color(0xFFF3F6F5),
    superficie = Color(0xFFFFFFFF),
    superficieAlt = Color(0xFFEAF0EF),
    borda = Color(0xFFDCE4E2),
    bordaSuave = Color(0xFFE6ECEB),
    texto = Color(0xFF142020),
    textoSuave = Color(0xFF4A5A58),
    base = Color(0xFF1F5F5B),
    sobreBase = Color(0xFFFFFFFF),
    baseForte = Color(0xFF123F3C),
    baseSuave = Color(0xFFE1F2EF),
    sobreBaseSuave = Color(0xFF145C54),
    acento = Color(0xFFD2604A),
    acentoTexto = Color(0xFFB4492F),
    acentoSuave = Color(0xFFFBE9E3),
    sobreAcentoSuave = Color(0xFF8A3622),
    destaque = Color(0xFF0F3D3A),
    sobreDestaque = Color(0xFFFFFFFF),
    sobreDestaqueSuave = Color(0xFFA8D8D0),
    menuFundo = Color(0xFF0B1615),
    menuCartao = Color(0xFF12211F),
    menuBorda = Color(0xFF1F3633),
    menuItemAtivo = Color(0xFF1C3533),
    menuTexto = Color(0xFFA9C3BF),
    menuTextoAtivo = Color(0xFFFFFFFF),
    menuTextoSuave = Color(0xFF7FA19C),
    menuAcento = Color(0xFFF08C74),
    menuVerde = Color(0xFF3FC6A8),
    info = CorDeStatus(Color(0xFFE5EEFB), Color(0xFF244E8F), Color(0xFF4C7CE0)),
    sucesso = CorDeStatus(Color(0xFFE3F4EA), Color(0xFF1C6B3E), Color(0xFF2E9E62)),
    aviso = CorDeStatus(Color(0xFFFBF0D7), Color(0xFF7A5410), Color(0xFFE9B44C)),
    erro = CorDeStatus(Color(0xFFFCE5E5), Color(0xFF9B2C2C), Color(0xFFD64545)),
    neutro = CorDeStatus(Color(0xFFECEFEE), Color(0xFF2E3E3C), Color(0xFF8C9C9A)),
)

val CoresEscuras = CoresTenviti(
    escuro = true,
    fundo = Color(0xFF0D1314),
    superficie = Color(0xFF141C1D),
    superficieAlt = Color(0xFF111819),
    borda = Color(0xFF24302F),
    bordaSuave = Color(0xFF1D2827),
    texto = Color(0xFFE6EEEC),
    textoSuave = Color(0xFF93A4A1),
    base = Color(0xFF3FC6A8),
    sobreBase = Color(0xFF062420),
    baseForte = Color(0xFF6FD3BF),
    baseSuave = Color(0xFF10302D),
    sobreBaseSuave = Color(0xFF8FE3D2),
    acento = Color(0xFFF08C74),
    acentoTexto = Color(0xFFF08C74),
    acentoSuave = Color(0xFF2A1712),
    sobreAcentoSuave = Color(0xFFF7B9A7),
    destaque = Color(0xFF10302D),
    sobreDestaque = Color(0xFFFFFFFF),
    sobreDestaqueSuave = Color(0xFF9FD3CC),
    menuFundo = Color(0xFF070C0C),
    menuCartao = Color(0xFF0F1A19),
    menuBorda = Color(0xFF1A2B29),
    menuItemAtivo = Color(0xFF16302D),
    menuTexto = Color(0xFFA9C3BF),
    menuTextoAtivo = Color(0xFFFFFFFF),
    menuTextoSuave = Color(0xFF7FA19C),
    menuAcento = Color(0xFFF08C74),
    menuVerde = Color(0xFF3FC6A8),
    info = CorDeStatus(Color(0xFF16264A), Color(0xFFA9C4F5), Color(0xFF6B95E8)),
    sucesso = CorDeStatus(Color(0xFF12301F), Color(0xFF8FDDB0), Color(0xFF4CC38A)),
    aviso = CorDeStatus(Color(0xFF3A2E12), Color(0xFFF2CF7E), Color(0xFFE9B44C)),
    erro = CorDeStatus(Color(0xFF3A1616), Color(0xFFF2A3A3), Color(0xFFE06464)),
    neutro = CorDeStatus(Color(0xFF1F2726), Color(0xFFC3D0CD), Color(0xFF6F7F7C)),
)

private val LocalCores = staticCompositionLocalOf { CoresClaras }

/** Acesso às cores da marca dentro de qualquer tela: `TenvitiTema.cores.base`. */
object TenvitiTema {
    val cores: CoresTenviti
        @Composable get() = LocalCores.current
}

/** Escolha de modo do painel: segue o sistema até a pessoa escolher; a escolha fica salva no aparelho. */
object PreferenciaTema {
    private const val CHAVE = "tenviti.tema"

    /** null = segue o sistema. */
    var escolhaEscuro by mutableStateOf(PreferenciasLocais.ler(CHAVE)?.let { it == "escuro" })
        private set

    fun alternar(atualEscuro: Boolean) {
        val novo = !atualEscuro
        escolhaEscuro = novo
        PreferenciasLocais.gravar(CHAVE, if (novo) "escuro" else "claro")
    }
}

@Composable
private fun fontesTitulo() = FontFamily(
    Font(Res.font.space_grotesk_medium, FontWeight.Medium),
    Font(Res.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(Res.font.space_grotesk_bold, FontWeight.Bold),
)

@Composable
private fun fontesTexto() = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
)

/** Família dos números e títulos (Space Grotesk), para valores em destaque fora do MaterialTheme. */
val FonteTitulo: FontFamily
    @Composable get() = fontesTitulo()

@Composable
fun TemaTenviti(conteudo: @Composable () -> Unit) {
    val escuro = PreferenciaTema.escolhaEscuro ?: isSystemInDarkTheme()
    val c = if (escuro) CoresEscuras else CoresClaras
    val titulo = fontesTitulo()
    val texto = fontesTexto()

    val tipografia = Typography(
        displaySmall = TextStyle(fontFamily = titulo, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.6).sp),
        headlineLarge = TextStyle(fontFamily = titulo, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
        headlineMedium = TextStyle(fontFamily = titulo, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = (-0.4).sp),
        headlineSmall = TextStyle(fontFamily = titulo, fontWeight = FontWeight.Bold, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = titulo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontFamily = titulo, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
        titleSmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
        bodyLarge = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = TextStyle(fontFamily = texto, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        labelSmall = TextStyle(fontFamily = texto, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    )

    val base = if (escuro) darkColorScheme() else lightColorScheme()
    val esquema = base.copy(
        primary = c.base,
        onPrimary = c.sobreBase,
        primaryContainer = c.baseSuave,
        onPrimaryContainer = c.sobreBaseSuave,
        secondary = c.acentoTexto,
        onSecondary = Color.White,
        secondaryContainer = c.acentoSuave,
        onSecondaryContainer = c.sobreAcentoSuave,
        tertiary = c.info.cheia,
        background = c.fundo,
        onBackground = c.texto,
        surface = c.superficie,
        onSurface = c.texto,
        surfaceVariant = c.superficieAlt,
        onSurfaceVariant = c.textoSuave,
        surfaceTint = c.base,
        surfaceContainerLowest = c.superficie,
        surfaceContainerLow = c.superficie,
        surfaceContainer = c.superficie,
        surfaceContainerHigh = c.superficie,
        surfaceContainerHighest = c.superficieAlt,
        inverseSurface = c.texto,
        inverseOnSurface = c.fundo,
        outline = c.borda,
        outlineVariant = c.bordaSuave,
        error = c.erro.cheia,
        onError = Color.White,
        errorContainer = c.erro.fundo,
        onErrorContainer = c.erro.texto,
        scrim = Color.Black,
    )

    val formas = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(18.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )

    CompositionLocalProvider(LocalCores provides c) {
        MaterialTheme(colorScheme = esquema, typography = tipografia, shapes = formas, content = conteudo)
    }
}
