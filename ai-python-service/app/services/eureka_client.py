"""
Eureka service discovery client
"""
import logging
import asyncio
import socket
from typing import Optional
import py_eureka_client.eureka_client as eureka_client
from py_eureka_client.eureka_client import EurekaClient as PyEurekaClient

from app.config.settings import Settings

logger = logging.getLogger(__name__)

class EurekaClient:
    """Eureka service discovery client"""
    
    def __init__(self, settings: Settings):
        self.settings = settings
        self.registered = False
        self.eureka_client: Optional[PyEurekaClient] = None
        self.registration_task: Optional[asyncio.Task] = None
    
    def _get_local_ip(self) -> str:
        """Get local IP address"""
        try:
            # Connect to a remote address to determine local IP
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
                s.connect(("8.8.8.8", 80))
                return s.getsockname()[0]
        except Exception:
            return "127.0.0.1"
    
    async def register(self):
        """Register service with Eureka"""
        if not self.settings.eureka_enabled:
            logger.info("Eureka registration disabled")
            return
        
        try:
            logger.info(f"Registering with Eureka server: {self.settings.eureka_server_url}")
            
            # Get local IP for registration
            local_ip = self._get_local_ip()
            
            # Initialize Eureka client
            await asyncio.get_event_loop().run_in_executor(
                None,
                self._init_eureka_client,
                local_ip
            )
            
            # Start heartbeat in background
            self.registration_task = asyncio.create_task(self._maintain_registration())
            
            self.registered = True
            logger.info(f"Successfully registered {self.settings.service_name} with Eureka")
            
        except Exception as e:
            logger.error(f"Failed to register with Eureka: {e}")
            self.registered = False
    
    def _init_eureka_client(self, local_ip: str):
        """Initialize Eureka client (blocking operation)"""
        self.eureka_client = eureka_client.init(
            eureka_server=self.settings.eureka_server_url,
            app_name=self.settings.service_name,
            instance_port=self.settings.port,
            instance_ip=local_ip,
            instance_host=local_ip,
            health_check_url=f"http://{local_ip}:{self.settings.port}/health/",
            status_page_url=f"http://{local_ip}:{self.settings.port}/health/detailed",
            home_page_url=f"http://{local_ip}:{self.settings.port}/",
            secure_port_enabled=False,
            renewal_interval_in_secs=self.settings.eureka_renewal_interval,
            duration_in_secs=self.settings.eureka_duration
        )
    
    async def _maintain_registration(self):
        """Maintain Eureka registration with periodic heartbeats"""
        while self.registered:
            try:
                await asyncio.sleep(self.settings.eureka_renewal_interval)
                if self.eureka_client and self.registered:
                    # The py-eureka-client handles heartbeats automatically
                    logger.debug("Eureka heartbeat maintained")
            except Exception as e:
                logger.error(f"Error maintaining Eureka registration: {e}")
                break
    
    async def deregister(self):
        """Deregister service from Eureka"""
        if not self.settings.eureka_enabled or not self.registered:
            return
        
        try:
            logger.info("Deregistering from Eureka...")
            
            # Cancel heartbeat task
            if self.registration_task:
                self.registration_task.cancel()
                try:
                    await self.registration_task
                except asyncio.CancelledError:
                    pass
            
            # Stop Eureka client
            if self.eureka_client:
                await asyncio.get_event_loop().run_in_executor(
                    None,
                    eureka_client.stop
                )
            
            self.registered = False
            logger.info("Successfully deregistered from Eureka")
            
        except Exception as e:
            logger.error(f"Error during Eureka deregistration: {e}")
    
    def is_registered(self) -> bool:
        """Check if service is registered with Eureka"""
        return self.registered