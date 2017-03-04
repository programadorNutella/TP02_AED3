import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Imagem
{
	public static void main (String args[])
	{
		Scanner entrada = new Scanner ( System.in );
		Imagem img = new Imagem();
		try
		{
			System.out.print( "Nome da imagem: " );
			String nome = entrada.nextLine();
			System.out.println( "Comprimindo..." );
			img.comprimir ( nome );
			System.out.println( "Descomprimindo..." );
			img.descomprimir ( nome + ".Z" );
			System.out.println( "Concluido." );
		}
		catch ( IOException ioe )
		{
			System.out.println ( "Finalizado." );
		}
	}
	
	/* Estrutura do arquivo comprimido:
	 * UTF - cabecalho ( identificador, dimensao, maximo valor de brilho )
	 * UTF - tabela de compressao, valores separados por ;
	 * int - contador de inteiros para representar a imagem em binario
	 * int - imagem comprimida ( varios ints ) em binario
	 */
	
	public void comprimir ( String nome ) throws IOException
	{
		// Dados
		DataOutputStream out = new DataOutputStream ( new FileOutputStream ( nome + ".Z" ) ); // Arquivo comprimido
		int[] mapa, frequencia; // Mapa de pixels, frequencia
		Scanner sc = new Scanner ( new File ( nome ) ); // Acessar imagem pgm
		String cabecalho = ""; // Identificador, dimensao, brilho separados por quebra de linha.
		
		// Huffman
		ArrayList <Arvore> lista = new ArrayList <Arvore>();
		String[] tabela;
		
		// Auxiliares
		String str = "";
		int contador = 0; // Ponteiro pro mapa de imagem
		int dimensao; // Dimensao da imagem ( largura * altura )
		
		// Identificador
		cabecalho = sc.nextLine() + '\n';
		
		// Ignora comentarios e pega dimensao
		str = sc.nextLine();
		while ( str.charAt(0) == '#' )
			str = sc.nextLine();
		cabecalho += str + '\n';
		tabela = str.split(" ");
		dimensao = Integer.parseInt( tabela[0] ) * Integer.parseInt( tabela[1] );
		
		// Cria tabela de frequencia com o valor de brilho
		str = sc.nextLine();
		frequencia = new int [ Integer.parseInt( str ) + 1 ];
		cabecalho += str;
		
		// Grava cabecalho no arquivo
		out.writeUTF( cabecalho );
		str = "";
		
		// Carrega mapa da imagem
		mapa = new int[ dimensao ];
		str = sc.nextLine();
		while ( sc.hasNextLine() )
		{
			tabela = str.split(" ");
			for ( int i = 0; i < tabela.length; i++ )
				if ( tabela[i].length() > 0 )
				{
					mapa[contador] = Integer.parseInt( tabela[i] );
					contador++;
				}
			str = sc.nextLine();
		}
		
		// Preenche lista de frequencias
		for ( int i = 0; i < mapa.length; i++ )
			frequencia[ mapa[i] ]++;
		
		// Cria arvore de Huffman
		for ( int i = 0; i < frequencia.length; i++ )
			if ( frequencia[i] > 0 )
				lista.add( new Arvore ( i, frequencia[i] ) );
		
		// Une arvores em uma so arvore
		while ( lista.size() > 1 )
		{ 
			for ( int i = 0; i < lista.size()-1; i++ )
				for ( int j = i+1; j < lista.size(); j++ )
					if ( lista.get(i).frequencia < lista.get(j).frequencia )
					{
						// ordena arvores por frequencia em modo decrescente
						Arvore aux = lista.get(i);
						lista.set(i, lista.get(j));
						lista.set(j, aux);						
					}
			lista.get( lista.size()-2 ).unir( lista.remove( lista.size()-1 ) );
		}
		
		// Preenche a tabela usando arvore
		int tamanho_tabela = Integer.parseInt( cabecalho.split("\n")[2] )+1;
		tabela = new String[ tamanho_tabela  ];
		lista.get(0).preencher( tabela, "" );
		
		for ( int i = 0; i < tamanho_tabela; i++ )
			if ( tabela[i] == null )
				tabela[i] = "";
		
		// Grava tabela no arquivo
		for ( int i = 0; i < tabela.length-1; i++ )
			str += tabela[i] + ';';
		str += tabela[ tabela.length-1 ];
		out.writeUTF( str );
		
		// Grava o numero de inteiros necessarios pra ler imagem
		out.writeInt( (int)Math.ceil( dimensao/4.0 ) );
		
		// Grava inteiros
		int valor = 0, cbits = 0; // cbits -> contador de bits
		for ( int i = 0; i < dimensao; i++ ) // codifica cada pixel
		{
			for ( int j = 0; j < tabela[ mapa[i] ].length(); j++ ) // percorre cada codigo e salva
			{
				valor = valor << 1;
				if ( tabela[ mapa[i] ].charAt(j) == '1' )
					valor = valor + 1;
				
				cbits ++;
				if ( cbits == 32 )
				{
					out.writeInt ( valor );	
					cbits = 0;
					valor = 0;
				}
			}
		}
		if ( cbits != 0 ) // Completa o ultimo inteiro e o salva.
		{
			valor = valor << 32-cbits;
			out.writeInt( valor );			
		}
		
		out.close(); // Fecha arquivo
	}
	
	public void descomprimir ( String nome ) throws IOException
	{
		int cPixels; // Contador descrescente de pixels
		DataInputStream in = new DataInputStream ( new FileInputStream ( nome ) );
		FileOutputStream out = new FileOutputStream ( nome.replace( ".pgm.Z", "" ) + "-2.pgm" );
		Padrao padrao;
		String str;
		
		// Grava cabecalho
		str = in.readUTF();
		out.write(( str + '\n' ).getBytes());
		
		// Le tabela de compressao
		padrao = new Padrao ( in.readUTF().split(";") );
		
		// Salva os pixels
		str = str.split("\n")[1];
		cPixels = Integer.parseInt(str.split(" ")[0]) * Integer.parseInt(str.split(" ")[1]);
		for ( int contador = in.readInt()-1; contador > 0; contador-- ) // Le quantidade de inteiros iniciais
		{
			str = Integer.toBinaryString(in.readInt()); // Le inteiro e converte pra string binario
			for ( int i = str.length(); i < 32; i++ ) // Completa a representacao do inteiro com 32 caracteres
				str = "0" + str;
			
			
			for ( int i = 0; i < str.length(); i++ ) // Procura por padrao
			{
				int posicao = padrao.checar( str.charAt(i) );
				if ( posicao != -1 ) // Padrao encontrado
				{
					out.write(( "" + posicao + " " ).getBytes()); // Salva pixel
					cPixels--;
					if ( cPixels == 0 )
						break;
					else if ( cPixels%16 == 0 )
						out.write('\n');
				}
			}
		}
		
		// Finaliza
		in.close();
		out.close();
	}
}




class Arvore
{
	public Arvore esq, dir;
	public int frequencia, valor;
	
	
	// Construtor - valor: tom de cinza; frequencia: frequencia do tom
	public Arvore ( int valor, int frequencia )
	{
		this.frequencia = frequencia;
		this.valor = valor;
	}
	
	
	// Preenche a tabela com os valores - tabela; caminho: "" (vazio)
	public void preencher ( String[] tabela, String caminho )
	{
		if ( valor != -1 )
			tabela [valor] = caminho;
		if ( esq != null )
			esq.preencher ( tabela, caminho + '0' );
		if ( dir != null )
			dir.preencher ( tabela, caminho + '1' );
	}
	
	
	// Junta dois elementos, criando um novo no.
	public void unir ( Arvore elemento )
	{
		Arvore novo = new Arvore ( this.valor, this.frequencia );
		novo.esq = this.esq;
		novo.dir = this.dir;
		this.esq = novo;
		this.dir = elemento;
		this.valor = -1;
		this.frequencia = this.frequencia + elemento.frequencia;
	}
}



class Padrao
{	
	ArvoreBinaria raiz, ponteiro; // no inicial, ponteiro atual
	
	// Construtor
	public Padrao ( String[] tabela )
	{
		for ( int i = 0; i < tabela.length; i++ )
			if ( tabela[i].length() > 0 )
			{
				if ( raiz == null )
					raiz = new ArvoreBinaria( );
				raiz.adicionar ( tabela[i], i );
			}
		ponteiro = raiz;
	}
	
	// Retorna posicao da tabela se bater com padrao, senao -1
	public int checar ( char letra )
	{
		try
		{
			if ( letra == '0' )
				ponteiro = ponteiro.esq;
			else
				ponteiro = ponteiro.dir;
			
			if ( ponteiro.elemento == -1 )
				return -1;
			else
			{
				int retorno = ponteiro.elemento;
				ponteiro = raiz;
				return retorno;
			}
		} catch ( NullPointerException e )
		{
			ponteiro = raiz;
			return 0;
		}
	}
}



class ArvoreBinaria
{
	public ArvoreBinaria esq, dir;
	public int elemento;
	
	public ArvoreBinaria ( )
	{
		this.elemento = -1; // Adiciona valor invalido
	}
	
	public void adicionar ( String cadeia, int elemento )
	{
		if ( cadeia.length() == 0 )
			this.elemento = elemento;
		else if ( cadeia.charAt(0) == '0' )
		{
			if ( esq == null )
				this.esq = new ArvoreBinaria ( );
			this.esq.adicionar( cadeia.substring (1, cadeia.length()), elemento );
		}
		else
		{
			if ( dir == null )
				this.dir = new ArvoreBinaria ( );
			this.dir.adicionar( cadeia.substring (1, cadeia.length()), elemento );
		}
	}
}
