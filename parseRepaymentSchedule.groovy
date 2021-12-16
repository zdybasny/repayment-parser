import groovy.io.FileType
import groovy.xml.XmlParser
import java.io.File

parseRepaymentSchedule('htmls')

void parseRepaymentSchedule(String pathDirectory) {
    ArrayList htmlFiles = loadHtmlFilesList(pathDirectory)
    ArrayList<Installment> installments = fetchInstallmentsFromHtmlFiles(htmlFiles)
    println "Installments count: ${installments.size()}"
    saveInstallmentsToXlsFile(installments)
}

ArrayList loadHtmlFilesList(String pathDirectory) {
    ArrayList files = []

    File dir = new File(pathDirectory)
    dir.eachFileRecurse(FileType.FILES) { file -> files << file }

    return files
}

class Installment {
    public String date
    public double capital
    public double interest
    public final double insurence = 232.72

}

ArrayList<Installment> fetchInstallmentsFromHtmlFiles(ArrayList files) {
    XmlParser parser = new XmlParser()
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

    ArrayList<Installment> installments = []
    println "     date     |    capital   |   interest"
    files.each { file ->
        try {
        Node bnpTableNode = parser.parse(file)
        installments += fetchInstallmentsFromNode_credit_table(bnpTableNode)
        } catch (err) {
            println("${file} ERROR: ${err}")

        }
    }
    return installments
}

ArrayList<Installment> fetchInstallmentsFromNode_credit_table(Node bnpTableNode) {
    return bnpTableNode.children()
        .findAll( node ->
            node.attribute('class') == 'bnp-table-row flex-row ng-scope'
        ).inject([]) { collection, node ->
            collection << fetchInstallmentFromNode_credit_table_row(node)
        }
}

Installment fetchInstallmentFromNode_credit_table_row(Node node) {
    Installment installment = new Installment()
    installment.date = fetchDateFromNode_bnp_table_row(node)
    installment.capital = fetchCapitalFromNode_bnp_table_row(node)
    installment.interest = fetchInterestFromNode_bnp_table_row(node)

    // println "  ${installment.date}  |    ${installment.capital}    |   ${installment.interest}"
    return installment
}
String fetchDateFromNode_bnp_table_row(Node node) {
    return node.children()[0].children()[0].children()[0].children()[0].text()
}

double fetchCapitalFromNode_bnp_table_row(Node node) {
    return fetchAmountFromColumnNode(node.children()[1])
}

double fetchInterestFromNode_bnp_table_row(Node node) {
    return fetchAmountFromColumnNode(node.children()[2])
}

double fetchAmountFromColumnNode(Node node) {
    ArrayList amountNodes = node.children()[0].children()[0].children()
    String wholePart = removeBracketsAndSpaces(amountNodes[0].value())
    String fractionPart = removeBracketsAndSpaces(amountNodes[1].value())
    String amount = "${wholePart}${fractionPart}".replace(',', '.')

    return amount as double
}

String removeBracketsAndSpaces(String value) {
    return value.replace('[', '').replace(']', '').replace(' ', '')
}

void saveInstallmentsToXlsFile(ArrayList<Installment> installments) {
    File file = new File('schedule.csv')
    BufferedWriter writter = file.newWriter()
    
    def FILE_HEADER = ['date', 'capital', 'interest', 'insurence']
    writter.append(FILE_HEADER.join(','))
    installments.each { installment ->
        writter.append("\n${installment.date},${installment.capital},${installment.interest},${installment.insurence}")
    }
    writter.close()
}
