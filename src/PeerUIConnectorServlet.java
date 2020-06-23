import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ranjabati sen
 */
public class PeerUIConnectorServlet extends HttpServlet {
	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	protected void processRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		DatagramSocket ds = new DatagramSocket();
		InetAddress ip = InetAddress.getLocalHost();
		int port;
		byte buf[] = null;
		byte[] receive = null;
		String filename = "";
		try (PrintWriter out = response.getWriter()) {
			/* TODO output your page here. You may use following sample code. */
			out.println("<!DOCTYPE html>");
			out.println("<html>");
			out.println("<head>");
			out.println("<title>File System</title>");
			out.println("</head>");
			String str = "\"background-image: url('source.gif');background-repeat: no-repeat;background-attachment: fixed;background-size: 100% 100%;font-weight: bold;\"";
			out.println("<body style=" + str + ">");
			filename = request.getParameter("filename");
			buf = filename.getBytes();
			port = Integer.parseInt(request.getParameter("port"));
			System.out.println("Sending message to port :" + port);
			DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, port);
			ds.send(DpSend);
			System.out.println("Sent file name/names :" + filename);
			String[] arr = filename.split(";");
			for (String file_name : arr) {
				// out.println("Chosen file name = " + file_name + "<br>");
				receive = new byte[2048];
				DatagramPacket DpReceive = null;
				DpReceive = new DatagramPacket(receive, receive.length);
				ds.receive(DpReceive);
				String receivedStr = data(receive).toString();
				System.out.println("Received output from filesystem "+receivedStr);
				String[] received_arr = receivedStr.split("\\|");
				for (String output : received_arr) {
					out.println(output + "<br>");
				}
				out.println("<br>");
			}
			out.println("</body>");
			out.println("</html>");
		}
	}

	public static StringBuilder data(byte[] a) {
		if (a == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;
	}

	// <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the
	// + sign on the left to edit the code.">
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Handles the HTTP <code>POST</code> method.
	 *
	 * @param request  servlet request
	 * @param response servlet response
	 * @throws ServletException if a servlet-specific error occurs
	 * @throws IOException      if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Returns a short description of the servlet.
	 *
	 * @return a String containing servlet description
	 */
	@Override
	public String getServletInfo() {
		return "Short description";
	}// </editor-fold>

}