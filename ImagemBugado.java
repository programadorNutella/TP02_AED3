import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Imagem
{
	// Nome original, identificador da imagem.
	private String nome, identificador;
	
	// valor maximo que representa o tom.
	private int tomMaximo;
	
	// {largura, altura}, {frequencia de cada tom}, {pixels}.
	private int[] dimensao, frequencia, mapa, mapaComprimido;
	
	// Tabela de compressao { valor do tom, codigo }
	private String tabela[];
	
	
	// Le imagem comprimida.
	public void lerComprimido ( String nome ) throws IOException
	{
		/* Estrutura do arquivo comprimodo:
			# identificador
			# dimensao
			# numero de tons ( n )
			# n valores separados por ; em mesma linha (tabela de conversao)
			# imagem codificada em binario
		*/
		
		FileInputStream in = new FileInputStream( new File (nome) ); // Arquivo comprimido
		byte[] buffer; // Bytes do arquivo comprimido
		Padrao padrao; // Encontra padroes
		
		//Auxiliares para setarem valores do cabecalho e usados para processar buffer
		String palavra = ""; 
		char letra;
		
		// Obtem identificador
		letra = (char)in.read();
		while ( letra != '\n' )
		{
			palavra += letra;
			letra = (char)in.read();
		}
		this.identificador = palavra;
		palavra = "";
		
		// Obtem dimensao da imagem
		letra = (char)in.read();
		while ( letra != '\n' )
		{
			palavra += letra;
			letra = (char)in.read();
		}
		this.dimensao = new int[2];
		dimensao[0] = Integer.parseInt(palavra.split(" ")[0]);
		dimensao[1] = Integer.parseInt(palavra.split(" ")[1]);
		palavra = "";
		
		// Obtem numero de tons
		letra = (char)in.read();
		while ( letra != '\n' )
		{
			palavra += letra;
			letra = (char)in.read();
		}
		this.tomMaximo = Integer.parseInt( palavra );
		palavra = "";
		
		// Obtem a tabela de codificacao
		letra = (char)in.read();
		while ( letra != '\n' )
		{
			palavra += letra;
			letra = (char)in.read();
		}
		//this.tabela = palavra.split(";");
		palavra = "";
		letra = ' ';
		
		// Inicializa mapa, buffer
		this.mapa = new int[ this.dimensao[0] * this.dimensao[1] ];
		buffer = new byte [ in.available() ]; // Comportar a parte binaria da imagem
		padrao = new Padrao ( tabela );
		
		// Le a parte binaria e fecha stream em seguida
		in.read( buffer );
		in.close();
		
		// Processa o buffer para reconstruir a imagem | ponteiro -> buffer; contador -> mapa
		int ponteiro = 0, contador = 0;
		while ( ponteiro/8 < buffer.length )
		{
			int byteSelecionado = buffer[ponteiro/8] >> 7-(ponteiro%8);
			letra = ( byteSelecionado%2 == 1)? '1':'0';
			int resposta = padrao.checar( letra );
			if ( resposta != -1 )
			{
				mapa[contador] = resposta;
				contador++;
			}
			ponteiro++;
		}
	}
	
	
	// Le imagem normal.
	public void lerImagem ( String nome ) throws IOException
	{
		ArrayList <Arvore> lista = new ArrayList <Arvore>(); //usada para Huffman
		Scanner sc = new Scanner ( new File ( nome ) );
		String linha;
		String[] vetor;
		this.nome = nome;
		
		// Identificador
		this.identificador = sc.nextLine();
		
		// Ignorar comentarios e pegar dimensao
		linha = sc.nextLine();
		while ( linha.charAt(0) == '#' ) { linha = sc.nextLine(); }
		vetor = linha.split(" ");
		this.dimensao = new int[2];
		this.dimensao[0] = Integer.parseInt( vetor[0] ); // largura
		this.dimensao[1] = Integer.parseInt( vetor[1] ); // altura
		
		// Pegar tom
		tomMaximo = Integer.parseInt(sc.nextLine());
		
		// Preencher mapa e frequencias
		int contador = 0; // ponteiro para mapa
		frequencia = new int [ tomMaximo + 1 ];
		mapa = new int [ this.dimensao[0] * this.dimensao[1] ];
		while ( sc.hasNextLine() )
		{
			vetor = sc.nextLine().split(" ");
			for ( int i = 0; i < vetor.length; i++ )
			{
				if ( vetor[i] != null )
				{
					int valor = Integer.parseInt( vetor[i] );
					frequencia[valor]++;
					mapa[contador] = valor;
					contador++;
				}
			}
		}
		
		// Criar arvore para Huffman. Arvore pronta estara na posicao 0.
		for ( int i = 0; i < frequencia.length; i++ )
			if ( frequencia[i] > 0 )
				lista.add( new Arvore ( i, frequencia[i] ) );
		
		int tamanho = lista.size();
		while ( tamanho > 1 )
		{ 
			for ( int i = 0; i < tamanho-1; i++ )
				for ( int j = i+1; j < tamanho; j++ )
					if ( lista.get(i).frequencia < lista.get(j).frequencia )
					{
						// ordena arvores por frequencia em modo decrescente
						Arvore aux = lista.get(i);
						lista.set(i, lista.get(j));
						lista.set(j, aux);						
					}
			
			lista.get( tamanho-2 ).unir( lista.remove( tamanho-1 ) );
			tamanho--;
		}
		
		// Preenche a tabela usando arvore
		tabela = new String[ tomMaximo + 1 ];
		lista.get(0).preencher( tabela, "" );
		
		for ( int i = 0; i < 256; i++ )
			if ( tabela[i] == null )
				tabela[i] = "";
	}
	
	
	// Grava a imagem comprimida.
	public void exportarComprimido ( ) throws IOException
	{
		/* Estrutura do arquivo comprimido:
			# identificador
			# dimensao
			# numero de tons ( n )
			# n valores separados por ; em mesma linha (tabela de conversao)
			# imagem codificada em binario
		*/
		
		FileOutputStream out = new FileOutputStream( "arq.pgm.Z" );
		byte[] buffer; // Vetor de bytes para ser salvo
		
		// Cabecalho
		out.write (( this.identificador + '\n' ).getBytes());
		out.write (( "" + this.dimensao[0] + ' ' + this.dimensao[1] + '\n' ).getBytes());
		out.write (( "" + this.tomMaximo + '\n' ).getBytes());
		
		// Tabela de codigos
		for ( int i = 0; i < this.tabela.length-1; i++ )
			out.write (( this.tabela[i] + ';' ).getBytes());
		out.write (( this.tabela[ this.tabela.length-1 ] + '\n').getBytes());
		
		// Cria buffer do tamanho certo
		int qtdBits = 0;
		for ( int i = 0; i < tomMaximo; i++ )
			qtdBits += frequencia[i] * tabela[i].length();
		
		if ( qtdBits%8 == 0 )
			buffer = new byte[ qtdBits/8 ];
		else
			buffer = new byte[ qtdBits/8 +1 ];
		
		// Adiciona bytes ao buffer
		int bit = 0;
		int pMapa = 0, contBits = 0; //ponteiro mapa, contador de bits
		while ( pMapa < mapa.length )
		{
			String codigo = tabela [ mapa[pMapa] ]; // Codigo do tom na tabela	
			for ( int i = 0; i < codigo.length(); i++ )
			{
				if ( qtdBits < contBits || codigo.charAt(i) == '0' )
					bit = bit << 1;
				else
					bit = (bit << 1) | 1;
				
				buffer[contBits/8] = (byte) bit; // Adiciona ao buffer
				
				contBits++;
			}
			pMapa++;
		}
		
		// Grava buffer e fecha o arquivo.
		out.write ( buffer );
		out.close();
	}
	
	
	// Grava a imagem normal.
	public void exportarImagem ( ) throws IOException
	{
		FileOutputStream out = new FileOutputStream( "arq.pgm" );
		out.write (( this.identificador + '\n' ).getBytes());
		out.write (( "" + this.dimensao[0] + ' ' + this.dimensao[1] + '\n' ).getBytes());
		out.write (( "" + this.tomMaximo + '\n' ).getBytes());
		
		//usados nos fors
		int i;
		int limite = mapa.length - 5; 
		
		for ( i = 0; i < limite; i+=5 ) // Grava de 5 em 5 para ganho de velocidade
			out.write(( "" + mapa[i] + ' ' + mapa[i+1] + ' ' + mapa[i+2] + ' ' + mapa[i+3] + ' ' + mapa[i+4] + " \n" ).getBytes());
		
		for ( i = i; i < mapa.length; i++ ) // Grava os 5 finais, 1 em 1.
			out.write(( "" + mapa[i] + ' ').getBytes());
		
		out.close();
	}
	
	
	public static void main ( String[] args )
	{
		try
		{
			//Scanner s = new Scanner ( System.in );
			Imagem img = new Imagem();
			img.lerImagem( "micrologo.pgm" );
			img.exportarComprimido();			
			img.lerComprimido( "arq.pgm.Z" );
			img.exportarImagem();
		}
		catch ( IOException e )
		{
			System.out.println ( "Erro ao abrir arquivo." );
		}
	}
}

