Hola!

Este es el proyecto final para el curso de Programación en Java de CoderHouse.

Consiste en crear un e-commerce en el cual tenemos 3 entidades, son ellas: Cliente, Producto y Venta(Comprobante).

Se utiliza un WebService externo para la fecha de la Venta que lo tenemos en la siguiente ubicación de GitHub: https://github.com/MartinPintos677/Fecha-WebService
Nota: el WebService externo va funcionar en el puerto 9090 mientras el proyecto que lo consume se usa en el puerto 8080.

En este proyecto hay una carpeta llamada Postman con un archivo donde está la colección para hacer las pruebas del sistema.

Primero se crea la base de datos en MySQL con el nombre coderventas

Como trabajar en Postman después de hacer funcionar el sistema(tanto en el puerto 8080 para el proyecto como en el 9090 para la fecha) ?
1. Crear un cliente.
2. Crear uno o más productos.
3. Generar la venta.

Se pueden hacer modificaciones en cliente o producto y también se pueden buscar el listado de los productos o clientes existentes, dado que la letra no pedía el delete de ninguna de 
las entidades, esta no fue implementada en el proyecto.

En la parte de Ventas, después de generadas se puede ver el listado de las ventas y también se puede buscar una venta específica por su ID.
