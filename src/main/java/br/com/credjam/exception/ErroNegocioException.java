package br.com.credjam.exception;

public class ErroNegocioException extends RuntimeException {
    private final String codigo;
    public ErroNegocioException(String codigo, String mensagem) { super(mensagem); this.codigo=codigo; }
    public String getCodigo(){ return codigo; }
}
