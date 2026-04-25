from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from utils.response import Response
from services.databaseConnection import ProductDB
import base64

router = APIRouter()

class ProductRequest(BaseModel):
    name: str
    price: int
    stock: int
    image: Optional[str] = None
    description: str
    gender: Optional[str] = 'U'

@router.get("/aily/admin/product/list")
def list_products():
    db = ProductDB()
    products = db.getAllProducts()
    
    # Map the tuple 
    product_list = []
    for p in products:
        img = p[4]
        if isinstance(img, bytes):
            img = base64.b64encode(img).decode('utf-8')
            
        product_list.append({
            "id": p[0],
            "name": p[1],
            "price": p[2],
            "stock": p[3],
            "image": img,
            "description": p[5],
            "gender": p[6],
        })

    return Response.Ok(data={"products": product_list})

@router.post("/aily/admin/product/add")
def add_product(product: ProductRequest):
    db = ProductDB()
    success = db.addProduct(
        name=product.name,
        price=product.price,
        stock=product.stock,
        image=product.image,
        description=product.description,
        gender=product.gender,
    )
    
    if success:
        return Response.Ok(data={"message": "Product added successfully"})
    else:
        return Response.Error(message="Failed to add product")

@router.put("/aily/admin/product/update/{product_id}")
def update_product(product_id: int, product: ProductRequest):
    db = ProductDB()
    existing = db.getProductById(product_id)
    if existing is None:
        return Response.NotFound("Product not found")

    success = db.updateProduct(
        product_id=product_id,
        name=product.name,
        price=product.price,
        stock=product.stock,
        image=product.image if product.image is not None else existing["image"],
        description=product.description,
        gender=product.gender
    )
    
    if success:
        return Response.Ok(data={"message": f"Product {product_id} updated successfully"})
    else:
        return Response.Error(message="Failed to update product")

@router.delete("/aily/admin/product/delete/{product_id}")
def delete_product(product_id: int):
    db = ProductDB()
    success = db.deleteProduct(product_id)
    
    if success:
        return Response.Ok(data={"message": f"Product {product_id} deleted successfully"})
    else:
        return Response.Error(message="Failed to delete product")

# Internal functions for chatbot integration
def perform_delete_product(product_id):
    db = ProductDB()
    success = db.deleteProduct(product_id)
    
    if success:
        return {"message": f"Produk dengan ID {product_id} berhasil dihapus.", "type": "delete"}
    else:
        return {"message": "Gagal menghapus produk.", "type": "delete"}

def perform_add_product(name, price, stock, description, image=None, gender='U'):
    db = ProductDB()
    success = db.addProduct(
        name=name,
        price=price,
        stock=stock,
        image=image,
        description=description,
        gender=gender
    )
    
    if success:
        return {"message": f"Produk '{name}' berhasil ditambahkan ke sistem.", "type": "add"}
    else:
        return {"message": "Gagal menambahkan produk.", "type": "add"}

def perform_update_product(product_id, name, price, stock, description, image=None, gender='U'):
    db = ProductDB()
    success = db.updateProduct(
        product_id=product_id,
        name=name,
        price=price,
        stock=stock,
        image=image,
        description=description,
        gender=gender
    )
    
    if success:
        return {"message": f"Produk dengan ID {product_id} berhasil diperbarui.", "type": "update"}
    else:
        return {"message": "Gagal memperbarui produk.", "type": "update"}

def perform_list_products():
    db = ProductDB()
    products = db.getAllProducts()
    
    # Map the tuple 
    product_list = []
    for p in products:
        img = p[4]
        if isinstance(img, bytes):
            img = base64.b64encode(img).decode('utf-8')
            
        product_list.append({
            "id": p[0],
            "name": p[1],
            "price": p[2],
            "stock": p[3],
            "image": img,
            "description": p[5],
            "gender": p[6]
        })

    return {"products": product_list, "type": "list"}
