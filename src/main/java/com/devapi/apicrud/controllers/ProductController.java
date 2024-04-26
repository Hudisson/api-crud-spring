package com.devapi.apicrud.controllers;

import java.util.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.net.MalformedURLException;

import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

//import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import com.devapi.apicrud.dtos.ProductImgDto;
import com.devapi.apicrud.dtos.ProductRecordDto;
import com.devapi.apicrud.models.ProductModel;
import com.devapi.apicrud.propeties.FileStorageProperties;
import com.devapi.apicrud.repositories.ProductRepository;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@SuppressWarnings("null")
@Controller
public class ProductController {

    @Autowired
    ProductRepository productRepository;

    private Path fileStorageLocation;

      public ProductController(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    /*Método para criar/salvar um produto */
    @PostMapping("/products")
    public ResponseEntity<ProductModel> saveProduct(@RequestBody @Valid ProductRecordDto productRecordDto){

        var productModel = new ProductModel();
        BeanUtils.copyProperties(productRecordDto, productModel);
        return ResponseEntity.status(HttpStatus.CREATED).body(productRepository.save(productModel));
    }

     /*Método para definir uma imagem para produto */
     @PutMapping("/products/img/{id}")
     public ResponseEntity<Object> uploadImg(@PathVariable(value = "id") UUID id,
                                            ProductRecordDto pRecordDto, @RequestParam("file") MultipartFile file){
            
            Optional<ProductModel> prodObj = productRepository.findById(id);
            if(prodObj.isEmpty()){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi possível atualizar Img. Produto não encontrado.");
            }

            String fileName = StringUtils.cleanPath(id+"_"+file.getOriginalFilename());

            try{
                Path targetLocation = fileStorageLocation.resolve(fileName);
                file.transferTo(targetLocation);

                String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/products/img/").path(fileName).toUriString();

                var productModel = prodObj.get();
    
                productModel.setPathImg(fileDownloadUri);

                BeanUtils.copyProperties(pRecordDto, productModel);

                return ResponseEntity.status(HttpStatus.OK).body(productRepository.save(productModel));

            }catch(IOException err){
                return ResponseEntity.ok("DEU ERRADO");
            } 
    }

    /*Método que obtem a imagem do produto pelo nome da imagem */
    @GetMapping("/products/img/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) throws IOException{

        Path filePath = fileStorageLocation.resolve(fileName).normalize();

        try {

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        
            if(contentType == null){
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment: fileName=\"" + resource.getFilename() + "\"")
                    .body(resource);
            
        } catch (MalformedURLException err) {
            return ResponseEntity.badRequest().build();
        }

    }

/*---------------------------------------------------------------------------------------------------- */

    /*Método que retorna todos os produtos */
    @GetMapping("/products")
    public ResponseEntity<List<ProductModel>> getAllProducts(){

        List<ProductModel> productList = productRepository.findAll();

        if(!productList.isEmpty()){
            for(ProductModel product : productList){
                UUID id = product.getIdProduct();
                product.add(linkTo(methodOn(ProductController.class).getOneProduct(id)).withSelfRel());
            }
        }

        return ResponseEntity.status(HttpStatus.OK).body(productList);
    }

    /* Método que retorna os nomes das imagens dos produtos */
    @GetMapping("/products/files")
    public ResponseEntity<List<String>> listFile() throws IOException{
        
        List<String> fileNames = Files.list(fileStorageLocation)
            .map(Path::getFileName)
            .map(Path::toString)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(fileNames);
    }

    /*Método que retorna um produto por ID */
    @GetMapping("/products/{id}")
    public ResponseEntity<Object> getOneProduct(@PathVariable(value = "id") UUID id){

        Optional<ProductModel> productObj = productRepository.findById(id);

        if(productObj.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produto não encontrado.");
        }

        productObj.get().add(linkTo(methodOn(ProductController.class).getAllProducts()).withSelfRel());

        return ResponseEntity.status(HttpStatus.OK).body(productObj.get());
    }

    /*Método para atualizar */
    @PutMapping("/products/{id}")
    public ResponseEntity<Object> updateProduct(@PathVariable(value = "id") UUID id, @RequestBody @Valid ProductRecordDto productRecordDto){
        Optional<ProductModel> producObj = productRepository.findById(id);
        if(producObj.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi possível atualizar. Produto não encontrado.");
        }

        var productModel = producObj.get();
        BeanUtils.copyProperties(productRecordDto, productModel);
        return ResponseEntity.status(HttpStatus.OK).body(productRepository.save(productModel));

    }

    /* Método para deletar */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Object> deleteProduct(@PathVariable(value = "id") UUID id){
        Optional<ProductModel> producObj = productRepository.findById(id);
        if(producObj.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Não foi possível deletar. Produto não encontrado.");
        }

        productRepository.delete(producObj.get());
        return ResponseEntity.status(HttpStatus.OK).body("Produto deletado com sucesso.");
    }
    
}
