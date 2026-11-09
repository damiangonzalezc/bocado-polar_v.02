// App.tsx · Rutas de la aplicación (tienda, panel admin y repartidor)

import React, { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { ToastProvider } from './components/ToastNotification';
import ProtectedRoute from './components/ProtectedRoute';
import StoreLayout from './components/StoreLayout';
import ReporteService from './services/reporteService';

// Pages
import Home from './pages/Home';
import Catalog from './pages/Catalog';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import Checkout from './pages/Checkout';
import Payment from './pages/Payment';
import Tracking from './pages/Tracking';
import Login from './pages/Login';
import Register from './pages/Register';
import AdminDashboard from './pages/AdminDashboard';
import Repartidor from './pages/Repartidor';
import NotFound from './pages/NotFound';

// Secciones del panel
import DashboardSection from './components/admin/DashboardSection';
import OrdersSection from './components/admin/OrdersSection';
import ProductsSection from './components/admin/ProductsSection';
import InventorySection from './components/admin/InventorySection';
import ColdChainSection from './components/admin/ColdChainSection';
import RoutesSection from './components/admin/RoutesSection';
import ClientsSection from './components/admin/ClientsSection';
import MarketingSection from './components/admin/MarketingSection';
import UsersSection from './components/admin/UsersSection';
import ConfigSection from './components/admin/ConfigSection';

const STAFF = ['ADMIN', 'VENDEDOR', 'BODEGUERO', 'SOPORTE'] as const;

const RedirigirLogin: React.FC = () => {
    const { search } = useLocation();
    return <Navigate to={`/login${search}`} replace />;
};

export const AppRoutes: React.FC = () => (
    <Routes>
        <Route element={<StoreLayout />}>
            <Route index element={<Home />} />
            <Route path="catalogo" element={<Catalog />} />
            <Route path="producto/:id" element={<ProductDetail />} />
            <Route path="carrito" element={<Cart />} />
            <Route path="login" element={<Login />} />
            <Route path="registro" element={<Register />} />
            {/* Enlace antiguo: /cuenta → /login (mantiene ?volver=) */}
            <Route path="cuenta" element={<RedirigirLogin />} />
            <Route path="pedido" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
            <Route path="pago" element={<ProtectedRoute roles={['CLIENTE']}><Payment /></ProtectedRoute>} />
            <Route path="seguimiento" element={<ProtectedRoute><Tracking /></ProtectedRoute>} />
            <Route path="*" element={<NotFound />} />
        </Route>

        <Route path="admin" element={<ProtectedRoute roles={[...STAFF]}><AdminDashboard /></ProtectedRoute>}>
            <Route index element={<DashboardSection />} />
            <Route path="pedidos" element={<OrdersSection />} />
            <Route path="catalogo" element={<ProductsSection />} />
            <Route path="inventario" element={<InventorySection />} />
            <Route path="frio" element={<ColdChainSection />} />
            <Route path="rutas" element={<RoutesSection />} />
            <Route path="clientes" element={<ClientsSection />} />
            <Route path="marketing" element={<MarketingSection />} />
            <Route path="usuarios" element={<UsersSection />} />
            <Route path="configuracion" element={<ConfigSection />} />
        </Route>

        <Route path="repartidor" element={<ProtectedRoute roles={['REPARTIDOR', 'ADMIN']}><Repartidor /></ProtectedRoute>} />
    </Routes>
);

const App: React.FC = () => {
    // Una visita por sesión del navegador (para la tasa de conversión de los KPI).
    useEffect(() => {
        void ReporteService.registrarVisita();
    }, []);

    return (
        <BrowserRouter>
            <ToastProvider>
                <AuthProvider>
                    <CartProvider>
                        <AppRoutes />
                    </CartProvider>
                </AuthProvider>
            </ToastProvider>
        </BrowserRouter>
    );
};

export default App;
