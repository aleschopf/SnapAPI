package com.tivit.snap_api.docs;

import com.tivit.snap_api.core.SnapRegistry;
import com.tivit.snap_api.core.SnapResourceMeta;
import com.tivit.snap_api.enums.Endpoint;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

@Configuration
public class SnapOpenApiConfig {

    @Bean
    public OpenAPI snapOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SnapREST API")
                        .description("API gerada automaticamente pelo SnapREST")
                        .version("1.0"))
                .components(new Components());
    }

    @Bean
    public OpenApiCustomizer snapApiCustomizer() {
        return openApi -> {
            Paths paths = new Paths();
            
            for (SnapResourceMeta meta : SnapRegistry.getAll()) {
                String path = normalizePath(meta.path());
                String tag = meta.entityClass().getSimpleName();

                if (meta.isEndpointEnabled(Endpoint.GET_ALL)) {
                    paths.addPathItem(path, buildGetAllPathItem(meta, tag));
                }

                if (meta.isEndpointEnabled(Endpoint.GET_BY_ID)) {
                    paths.addPathItem(path + "/{id}", buildGetByIdPathItem(meta, tag));
                }

                if (meta.isEndpointEnabled(Endpoint.CREATE)) {
                    paths.addPathItem(path, buildPostPathItem(openApi, meta, tag));
                }

                if (meta.isEndpointEnabled(Endpoint.EDIT)) {
                    paths.addPathItem(path + "/{id}", buildPutPathItem(openApi, meta, tag));
                }

                if (meta.isEndpointEnabled(Endpoint.DELETE)) {
                    paths.addPathItem(path + "/{id}", buildDeletePathItem(meta, tag));
                }
            }
            
            openApi.paths(paths);
        };
    }
    
    private PathItem buildGetAllPathItem(SnapResourceMeta meta, String tag) {
        return new PathItem().get(new Operation()
                .tags(List.of(tag))
                .summary("Listar todos os " + tag)
                .description("Retorna uma lista paginada de " + tag)
                .parameters(List.of(
                        new Parameter().name("page").in("query").description("Número da página"),
                        new Parameter().name("size").in("query").description("Tamanho da página"),
                        new Parameter().name("sort").in("query").description("Critério de ordenação")
                ))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("Lista de " + tag + " retornada com sucesso")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref("#/components/schemas/Page_" + tag)))))));
    }
    
    private PathItem buildGetByIdPathItem(SnapResourceMeta meta, String tag) {
        return new PathItem().get(new Operation()
                .tags(List.of(tag))
                .summary("Obter " + tag + " por ID")
                .description("Retorna um único " + tag + " pelo seu identificador")
                .parameters(List.of(
                        new Parameter().name("id").in("path").required(true).description("ID do " + tag)
                ))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description(tag + " encontrado")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>()
                                                        .$ref("#/components/schemas/" + tag)))))
                        .addApiResponse("404", new ApiResponse()
                                .description(tag + " não encontrado"))));
    }
    
    private PathItem buildPostPathItem(OpenAPI openApi, SnapResourceMeta meta, String tag) {
        Schema<?> schema = resolveSchema(openApi, meta.entityClass());
        
        return new PathItem().post(new Operation()
                .tags(List.of(tag))
                .summary("Criar novo " + tag)
                .description("Cria uma nova instância de " + tag)
                .requestBody(new RequestBody()
                        .description(tag + " a ser criado")
                        .content(new Content()
                                .addMediaType("application/json", 
                                        new MediaType().schema(schema)))
                        .required(true))
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse()
                                .description(tag + " criado com sucesso")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(schema))))));
    }
    
    private PathItem buildPutPathItem(OpenAPI openApi, SnapResourceMeta meta, String tag) {
        Schema<?> schema = resolveSchema(openApi, meta.entityClass());
        
        return new PathItem().put(new Operation()
                .tags(List.of(tag))
                .summary("Atualizar " + tag)
                .description("Atualiza um " + tag + " existente")
                .parameters(List.of(
                        new Parameter().name("id").in("path").required(true).description("ID do " + tag)
                ))
                .requestBody(new RequestBody()
                        .description(tag + " com dados atualizados")
                        .content(new Content()
                                .addMediaType("application/json", 
                                        new MediaType().schema(schema)))
                        .required(true))
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description(tag + " atualizado com sucesso")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(schema))))
                        .addApiResponse("404", new ApiResponse()
                                .description(tag + " não encontrado"))));
    }
    
    private PathItem buildDeletePathItem(SnapResourceMeta meta, String tag) {
        return new PathItem().delete(new Operation()
                .tags(List.of(tag))
                .summary("Remover " + tag)
                .description("Remove um " + tag + " existente")
                .parameters(List.of(
                        new Parameter().name("id").in("path").required(true).description("ID do " + tag)
                ))
                .responses(new ApiResponses()
                        .addApiResponse("204", new ApiResponse()
                                .description(tag + " removido com sucesso"))
                        .addApiResponse("404", new ApiResponse()
                                .description(tag + " não encontrado"))));
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    private Schema resolveSchema(OpenAPI openApi, Class<?> entityClass) {
        String schemaName = entityClass.getSimpleName();
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        
        if (!schemas.containsKey(schemaName)) {
            Schema schema = new Schema<>()
                    .type("object")
                    .name(schemaName)
                    .title(schemaName);

            for (Field field : entityClass.getDeclaredFields()) {
                String fieldName = field.getName();
                Class<?> fieldType = field.getType();
                
                Schema fieldSchema = new Schema<>()
                        .type(getSchemaType(fieldType))
                        .example(getExampleValue(fieldType));
                
                schema.addProperty(fieldName, fieldSchema);
            }
            
            openApi.getComponents().addSchemas(schemaName, schema);

            String pageSchemaName = "Page_" + schemaName;
            Schema pageSchema = new Schema<>()
                    .type("object")
                    .name(pageSchemaName)
                    .title(pageSchemaName)
                    .addProperty("content", new Schema<>()
                            .type("array")
                            .items(new Schema<>().$ref("#/components/schemas/" + schemaName)))
                    .addProperty("totalElements", new Schema<>().type("integer"))
                    .addProperty("totalPages", new Schema<>().type("integer"))
                    .addProperty("size", new Schema<>().type("integer"))
                    .addProperty("number", new Schema<>().type("integer"));
            
            openApi.getComponents().addSchemas(pageSchemaName, pageSchema);
        }
        
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }
    
    private String getSchemaType(Class<?> fieldType) {
        if (fieldType == String.class) return "string";
        if (fieldType == Integer.class || fieldType == int.class) return "integer";
        if (fieldType == Long.class || fieldType == long.class) return "integer";
        if (fieldType == Double.class || fieldType == double.class) return "number";
        if (fieldType == Boolean.class || fieldType == boolean.class) return "boolean";
        if (fieldType.isEnum()) return "string";
        return "object";
    }
    
    private Object getExampleValue(Class<?> fieldType) {
        if (fieldType == String.class) return "string-example";
        if (fieldType == Integer.class || fieldType == int.class) return 123;
        if (fieldType == Long.class || fieldType == long.class) return 123456L;
        if (fieldType == Double.class || fieldType == double.class) return 123.45;
        if (fieldType == Boolean.class || fieldType == boolean.class) return true;
        if (fieldType.isEnum()) return fieldType.getEnumConstants()[0].toString();
        return null;
    }
    
    private String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }
}