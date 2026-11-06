package com.example.DrogueriaLosTresHermanos.servicio;

import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Categoria;
import com.example.DrogueriaLosTresHermanos.servicio.CatalogoProductos.Producto;

import java.util.List;

/** Dónde se guarda el catálogo: en MySQL cuando hay conexión configurada, o en un archivo local. */
public interface AlmacenCatalogo {

    record Contenido(List<Categoria> categorias, List<Producto> productos) {}

    Contenido cargar();

    /** Guarda el producto y devuelve su identificador definitivo. */
    int guardarProducto(Producto producto, boolean nuevo);

    void cambiarFoto(int id, String foto);

    void cambiarVisibilidad(int id, boolean activo);

    void guardarCategoria(Categoria categoria, boolean nueva);

    void eliminarCategoria(String id);

    /** Se llama después de cada cambio con el catálogo completo; el archivo lo usa para escribirse. */
    default void sincronizar(Contenido todo) {
    }

    /** Verdadero si los datos viven fuera de la memoria y hay que volver a leerlos después de cambiarlos. */
    default boolean recargarTrasCambios() {
        return false;
    }

    /** Nombre corto del origen, para los registros de arranque. */
    String origen();
}
