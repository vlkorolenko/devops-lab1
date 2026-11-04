// State
let allProducts = [];
let cart = [];
let selectedCategory = null;
let searchQuery = "";

// Initialize
document.addEventListener("DOMContentLoaded", () => {
    loadProducts();
    loadCategories();
    checkHealth();
    setupSearch();
    setInterval(checkHealth, 15000); // Check service health every 15s
    lucide.createIcons();
});

// Fetch products from Product Microservice
async function loadProducts() {
    const grid = document.getElementById("productGrid");
    try {
        let url = "/api/products";
        const params = new URLSearchParams();
        if (selectedCategory) params.append("category", selectedCategory);
        if (searchQuery) params.append("search", searchQuery);
        if (params.toString()) url += `?${params.toString()}`;

        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        allProducts = await res.json();

        renderProducts(allProducts);
        document.getElementById("productCount").innerText = allProducts.length;
    } catch (err) {
        console.error("Error loading products:", err);
        grid.innerHTML = `
            <div class="col-span-full p-8 text-center bg-rose-50 rounded-2xl border border-rose-200 text-rose-700">
                <i data-lucide="alert-triangle" class="w-8 h-8 mx-auto mb-2 text-rose-500"></i>
                <h3 class="font-bold text-base">Помилка завантаження каталогу</h3>
                <p class="text-xs text-rose-600 mt-1">Не вдалося зв'язатися з Product Service. Переконайтеся, що бекенд запущено.</p>
                <button onclick="loadProducts()" class="mt-4 px-4 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-medium transition">
                    Повторити спробу
                </button>
            </div>
        `;
        lucide.createIcons();
    }
}

// Render Products Grid
function renderProducts(products) {
    const grid = document.getElementById("productGrid");
    if (!products || products.length === 0) {
        grid.innerHTML = `
            <div class="col-span-full py-16 text-center text-slate-400">
                <i data-lucide="inbox" class="w-12 h-12 mx-auto mb-2 opacity-50"></i>
                <p class="text-base font-medium">Товарів не знайдено</p>
                <p class="text-xs mt-1">Спробуйте змінити пошуковий запит або категорію</p>
            </div>
        `;
        lucide.createIcons();
        return;
    }

    grid.innerHTML = products.map(product => `
        <div class="product-card bg-white rounded-2xl border border-slate-200 overflow-hidden flex flex-col">
            <div class="relative h-48 bg-slate-100 overflow-hidden">
                <img src="${product.imageUrl || 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?w=500'}" 
                     alt="${escapeHtml(product.name)}" 
                     class="w-full h-full object-cover">
                <span class="absolute top-3 left-3 px-2.5 py-1 rounded-full text-[11px] font-semibold bg-white/90 backdrop-blur-md text-slate-800 shadow-sm">
                    ${escapeHtml(product.category)}
                </span>
                <span class="absolute top-3 right-3 px-2 py-0.5 rounded-full text-[10px] font-bold ${product.stock > 0 ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}">
                    ${product.stock > 0 ? `В наявності: ${product.stock}` : 'Немає в наявності'}
                </span>
            </div>
            <div class="p-5 flex-1 flex flex-col">
                <h3 class="font-bold text-base text-slate-900 mb-1 leading-snug">${escapeHtml(product.name)}</h3>
                <p class="text-slate-500 text-xs line-clamp-2 mb-4 flex-1">${escapeHtml(product.description || '')}</p>
                
                <div class="flex items-center justify-between pt-3 border-t border-slate-100">
                    <div>
                        <span class="text-xs text-slate-400">Ціна</span>
                        <div class="text-lg font-extrabold text-slate-900">$${Number(product.price).toFixed(2)}</div>
                    </div>
                    <button onclick="addToCart(${product.id})" 
                            class="flex items-center space-x-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-sm transition active:scale-95">
                        <i data-lucide="plus" class="w-4 h-4"></i>
                        <span>В кошик</span>
                    </button>
                </div>
            </div>
        </div>
    `).join("");
    lucide.createIcons();
}

// Load Categories
async function loadCategories() {
    try {
        const res = await fetch("/api/products/categories");
        if (!res.ok) return;
        const categories = await res.json();
        const pillsContainer = document.getElementById("categoryPills");

        let html = `
            <button onclick="filterCategory(null)" class="category-btn ${!selectedCategory ? 'active bg-indigo-600 text-white' : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'} px-4 py-1.5 text-xs font-medium rounded-full transition">
                Всі товари
            </button>
        `;

        categories.forEach(cat => {
            const isActive = selectedCategory === cat;
            html += `
                <button onclick="filterCategory('${escapeHtml(cat)}')" class="category-btn ${isActive ? 'active bg-indigo-600 text-white' : 'bg-white border border-slate-200 text-slate-700 hover:bg-slate-100'} px-4 py-1.5 text-xs font-medium rounded-full transition">
                    ${escapeHtml(cat)}
                </button>
            `;
        });
        pillsContainer.innerHTML = html;
    } catch (e) {
        console.error("Failed to load categories", e);
    }
}

function filterCategory(cat) {
    selectedCategory = cat;
    loadCategories();
    loadProducts();
}

function resetFilters() {
    selectedCategory = null;
    searchQuery = "";
    document.getElementById("searchInput").value = "";
    loadCategories();
    loadProducts();
}

function setupSearch() {
    const input = document.getElementById("searchInput");
    let timer;
    input.addEventListener("input", (e) => {
        clearTimeout(timer);
        timer = setTimeout(() => {
            searchQuery = e.target.value.trim();
            loadProducts();
        }, 300);
    });
}

// Cart Operations
function addToCart(productId) {
    const product = allProducts.find(p => p.id === productId);
    if (!product) return;

    const existing = cart.find(item => item.productId === productId);
    if (existing) {
        existing.quantity += 1;
    } else {
        cart.push({
            productId: product.id,
            productName: product.name,
            price: Number(product.price),
            imageUrl: product.imageUrl,
            quantity: 1
        });
    }

    updateCartBadge();
    showToast(`"${product.name}" додано до кошика!`, "success");
}

function updateCartQuantity(productId, delta) {
    const item = cart.find(i => i.productId === productId);
    if (!item) return;

    item.quantity += delta;
    if (item.quantity <= 0) {
        cart = cart.filter(i => i.productId !== productId);
    }
    updateCartBadge();
    renderCart();
}

function removeFromCart(productId) {
    cart = cart.filter(i => i.productId !== productId);
    updateCartBadge();
    renderCart();
}

function updateCartBadge() {
    const badge = document.getElementById("cartCountBadge");
    const count = cart.reduce((sum, item) => sum + item.quantity, 0);
    if (count > 0) {
        badge.innerText = count;
        badge.classList.remove("hidden");
    } else {
        badge.classList.add("hidden");
    }
}

function renderCart() {
    const container = document.getElementById("cartItemsContainer");
    const totalEl = document.getElementById("cartTotalPrice");
    const form = document.getElementById("checkoutForm");

    if (cart.length === 0) {
        container.innerHTML = `
            <div class="py-16 text-center text-slate-400">
                <i data-lucide="shopping-cart" class="w-12 h-12 mx-auto mb-2 opacity-40"></i>
                <p class="font-medium text-sm">Кошик порожній</p>
                <p class="text-xs text-slate-400 mt-1">Оберіть товари з каталогу магазину</p>
            </div>
        `;
        totalEl.innerText = "$0.00";
        form.classList.add("opacity-50", "pointer-events-none");
        lucide.createIcons();
        return;
    }

    form.classList.remove("opacity-50", "pointer-events-none");

    let total = 0;
    container.innerHTML = cart.map(item => {
        const itemTotal = item.price * item.quantity;
        total += itemTotal;
        return `
            <div class="flex items-center justify-between p-3 bg-slate-50 rounded-xl border border-slate-200">
                <div class="flex-1 pr-3">
                    <h4 class="font-semibold text-xs text-slate-900">${escapeHtml(item.productName)}</h4>
                    <span class="text-xs text-indigo-600 font-bold">$${item.price.toFixed(2)}</span>
                </div>
                <div class="flex items-center space-x-2">
                    <button onclick="updateCartQuantity(${item.productId}, -1)" class="w-6 h-6 rounded-md bg-white border border-slate-300 flex items-center justify-center text-slate-600 hover:bg-slate-100 text-xs font-bold">-</button>
                    <span class="text-xs font-bold w-5 text-center">${item.quantity}</span>
                    <button onclick="updateCartQuantity(${item.productId}, 1)" class="w-6 h-6 rounded-md bg-white border border-slate-300 flex items-center justify-center text-slate-600 hover:bg-slate-100 text-xs font-bold">+</button>
                    <button onclick="removeFromCart(${item.productId})" class="ml-2 text-rose-500 hover:text-rose-700 p-1">
                        <i data-lucide="trash-2" class="w-4 h-4"></i>
                    </button>
                </div>
            </div>
        `;
    }).join("");

    totalEl.innerText = `$${total.toFixed(2)}`;
    lucide.createIcons();
}

function openCartModal() {
    renderCart();
    document.getElementById("cartModal").classList.remove("hidden");
}

function closeCartModal() {
    document.getElementById("cartModal").classList.add("hidden");
}

// Place Order to Order Microservice
async function submitOrder(e) {
    e.preventDefault();
    if (cart.length === 0) return;

    const btn = document.getElementById("submitOrderBtn");
    btn.disabled = true;
    btn.innerHTML = `<span>Обробка...</span>`;

    const orderData = {
        customerName: document.getElementById("custName").value.trim(),
        customerEmail: document.getElementById("custEmail").value.trim(),
        customerAddress: document.getElementById("custAddress").value.trim(),
        items: cart.map(i => ({
            productId: i.productId,
            productName: i.productName,
            price: i.price,
            quantity: i.quantity
        }))
    };

    try {
        const res = await fetch("/api/orders", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(orderData)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || "Помилка при створенні замовлення");
        }

        const createdOrder = await res.json();
        cart = [];
        updateCartBadge();
        closeCartModal();
        showToast(`Замовлення #${createdOrder.id} успішно створено! Сума: $${createdOrder.totalAmount}`, "success");
        openOrdersModal();
    } catch (err) {
        console.error("Order error:", err);
        showToast(err.message || "Не вдалося оформити замовлення", "error");
    } finally {
        btn.disabled = false;
        btn.innerHTML = `<span>Оформити замовлення</span><i data-lucide="arrow-right" class="w-4 h-4"></i>`;
        lucide.createIcons();
    }
}

// Orders Modal
async function openOrdersModal() {
    const modal = document.getElementById("ordersModal");
    const container = document.getElementById("ordersContainer");
    modal.classList.remove("hidden");

    container.innerHTML = `
        <div class="py-12 text-center text-slate-400">
            <div class="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600 mb-2"></div>
            <p class="text-xs">Завантаження замовлень...</p>
        </div>
    `;

    try {
        const res = await fetch("/api/orders");
        if (!res.ok) throw new Error("HTTP " + res.status);
        const orders = await res.json();

        if (orders.length === 0) {
            container.innerHTML = `
                <div class="py-12 text-center text-slate-400">
                    <i data-lucide="package-open" class="w-12 h-12 mx-auto mb-2 opacity-40"></i>
                    <p class="font-medium text-sm">Немає збережених замовлень</p>
                    <p class="text-xs text-slate-400 mt-1">Оформіть ваше перше замовлення через кошик</p>
                </div>
            `;
            lucide.createIcons();
            return;
        }

        container.innerHTML = orders.map(o => `
            <div class="p-4 rounded-xl border border-slate-200 bg-slate-50">
                <div class="flex items-center justify-between border-b border-slate-200 pb-2 mb-3">
                    <div>
                        <div class="flex items-center space-x-2">
                            <span class="font-bold text-slate-900">Замовлення #${o.id}</span>
                            <span class="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800">${o.status}</span>
                        </div>
                        <div class="text-[11px] text-slate-400 mt-0.5">Клієнт: ${escapeHtml(o.customerName)} (${escapeHtml(o.customerEmail)})</div>
                    </div>
                    <div class="text-right">
                        <span class="text-xs text-slate-400">Разом</span>
                        <div class="text-base font-bold text-indigo-600">$${Number(o.totalAmount).toFixed(2)}</div>
                    </div>
                </div>
                <div class="space-y-1">
                    ${(o.items || []).map(i => `
                        <div class="flex justify-between text-xs text-slate-600">
                            <span>${i.quantity}x ${escapeHtml(i.productName)}</span>
                            <span class="font-medium">$${(i.price * i.quantity).toFixed(2)}</span>
                        </div>
                    `).join("")}
                </div>
                <div class="mt-3 pt-2 border-t border-slate-200/60 text-[11px] text-slate-400">
                    Адреса доставки: ${escapeHtml(o.customerAddress)}
                </div>
            </div>
        `).join("");
        lucide.createIcons();
    } catch (err) {
        container.innerHTML = `
            <div class="p-4 text-center text-rose-600 text-xs">
                Помилка підключення до Order Microservice.
            </div>
        `;
    }
}

function closeOrdersModal() {
    document.getElementById("ordersModal").classList.add("hidden");
}

// Health check badge
async function checkHealth() {
    const badge = document.getElementById("healthBadge");
    const text = document.getElementById("healthText");
    try {
        const [prodRes, ordRes] = await Promise.all([
            fetch("/api/products"),
            fetch("/api/orders")
        ]);
        if (prodRes.ok && ordRes.ok) {
            badge.className = "hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200";
            text.innerText = "Всі сервіси UP";
        } else {
            badge.className = "hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200";
            text.innerText = "Деградація сервісів";
        }
    } catch (e) {
        badge.className = "hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200";
        text.innerText = "Сервіси недоступні";
    }
}

// Toast
function showToast(message, type = "info") {
    const container = document.getElementById("toastContainer");
    const toast = document.createElement("div");
    const bg = type === "success" ? "bg-emerald-600" : type === "error" ? "bg-rose-600" : "bg-slate-800";
    toast.className = `toast pointer-events-auto px-4 py-2.5 rounded-xl shadow-lg text-white text-xs font-medium flex items-center space-x-2 ${bg}`;
    toast.innerHTML = `<span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = "0";
        toast.style.transition = "opacity 0.3s";
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
